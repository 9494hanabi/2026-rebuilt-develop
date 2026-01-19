#!/usr/bin/env bash
set -euo pipefail

# =========================
# git-assist.sh
# Interactive helper for:
#  - commit & push
#  - merge
#  - pull
# =========================

# ---------- helpers ----------
die() { echo "❌ $*" >&2; exit 1; }

need_git_repo() {
  git rev-parse --is-inside-work-tree >/dev/null 2>&1 || die "ここはGitリポジトリではありません。"
}

current_branch() {
  git rev-parse --abbrev-ref HEAD
}

has_changes() {
  [[ -n "$(git status --porcelain)" ]]
}

prompt() {
  local msg="$1"
  local default="${2:-}"
  local ans=""
  if [[ -n "$default" ]]; then
    read -r -p "$msg [$default]: " ans
    ans="${ans:-$default}"
  else
    read -r -p "$msg: " ans
  fi
  echo "$ans"
}

prompt_yn() {
  local msg="$1"
  local default="${2:-Y}" # Y or N
  local ans=""
  read -r -p "$msg [${default}/$( [[ "$default" == "Y" ]] && echo "n" || echo "y" )]: " ans
  ans="${ans:-$default}"
  case "${ans,,}" in
    y|yes) return 0 ;;
    n|no)  return 1 ;;
    *)     # if weird input, use default
           [[ "$default" == "Y" ]] && return 0 || return 1 ;;
  esac
}

select_one() {
  local title="$1"; shift
  local -a options=("$@")
  local PS3="番号を選んでください: "

  echo "" >&2
  echo "🧩 $title" >&2

  select opt in "${options[@]}"; do
    if [[ -n "${opt:-}" ]]; then
      printf "%s\n" "$opt"   # ← 戻り値はstdout
      return 0
    fi
    echo "もう一度選んでね。" >&2
  done
}

ensure_clean_or_confirm() {
  if has_changes; then
    echo ""
    echo "📌 現在の変更があります:"
    git status --short
    echo ""
    if ! prompt_yn "このまま進めますか？" "Y"; then
      die "中断しました。"
    fi
  fi
}

# ---------- commit message builder ----------
map_scope() {
  local s="$1"
  case "$s" in
    ドライブ) echo "drive" ;;
    ビジョン) echo "vision" ;;
    オート) echo "auto" ;;
    タレット) echo "talet" ;;
    シューター) echo "shooter" ;;
    インテーク) echo "intake" ;;
    クライム) echo "clime" ;;
    その他の機能) echo "misc" ;;
    *) echo "misc" ;;
  esac
}

map_type() {
  local t="$1"
  case "$t" in
    エディット) echo "feat" ;;             # 新規/改善もここに寄せる
    リファクタリング\(整形\)) echo "refactor" ;;
    デバッグ) echo "fix" ;;
    テスト) echo "test" ;;
    *) echo "chore" ;;
  esac
}

build_commit_message() {
  local op="$1"

  local action
  action="$(select_one "編集した機能を教えて下さい。" "ドライブ" "ビジョン" "シューター" "インテーク" "その他の機能")"
  local feature_name
  feature_name="$(prompt "機能の名前を教えて下さい" "例: クライム")"
  local edit_kind
  edit_kind="$(select_one "編集内容を教えて下さい" "エディット" "リファクタリング(整形)" "デバッグ" "テスト")"
  local detail
  detail="$(prompt "編集内容の詳細を教えて下さい" "例: L1に登る機能を作成した。")"
  local stability
  stability="$(select_one "コードの状態を教えて下さい。" "安定" "バグ有り")"

  local tag_line=""
  if prompt_yn "タグを打ちますか？" "N"; then
    local tags
    tags="$(prompt "タグ(カンマ区切り)を入力" "例: climb,auto")"
    # normalize spaces
    tags="$(echo "$tags" | tr -d ' ' )"
    tag_line="Tags: $tags"
  fi

  local type scope status
  type="$(map_type "$edit_kind")"
  scope="$(map_scope "$action")"
  status="$([[ "$stability" == "安定" ]] && echo "stable" || echo "buggy")"

  # Subject (1行目)
  local subject="${type}(${scope}): ${feature_name} - ${detail} [${status}]"

  # Body (2行目以降)
  local body=""
  body+="Operation: ${op}\n"
  body+="Category: ${action}\n"
  body+="Feature: ${feature_name}\n"
  body+="Change: ${edit_kind}\n"
  body+="Status: ${stability}\n"
  if [[ -n "$tag_line" ]]; then
    body+="${tag_line}\n"
  fi

  echo -e "${subject}\n\n${body}"
}

# ---------- operations ----------
do_commit_and_push() {
  need_git_repo

  echo ""
  echo "🧾 Commit & Push を開始します。"
  echo "現在のブランチ: $(current_branch)"
  echo ""

  if ! has_changes; then
    echo "✅ 変更がありません (git status が空)。コミット不要です。"
    return 0
  fi

  git status --short
  echo ""

  if prompt_yn "git add -A (全変更をステージ) しますか？" "Y"; then
    git add -A
  else
    echo "ℹ️ ステージしない場合、手動で git add してからもう一度実行してね。"
    die "中断しました。"
  fi

  local msg
  msg="$(build_commit_message "commit-push")"

  echo ""
  echo "📝 生成されたコミットメッセージ:"
  echo "------------------------------"
  echo "$msg"
  echo "------------------------------"
  echo ""

  if ! prompt_yn "このメッセージで commit しますか？" "Y"; then
    die "中断しました。"
  fi

  git commit -m "$(echo "$msg" | head -n 1)" -m "$(echo "$msg" | tail -n +3)"

  # push (set upstream if needed)
  local branch
  branch="$(current_branch)"

  echo ""
  echo "🚀 push します: origin ${branch}"

  if git rev-parse --abbrev-ref --symbolic-full-name "@{u}" >/dev/null 2>&1; then
    git push
  else
    git push -u origin "$branch"
  fi

  echo "✅ 完了: commit & push"

  # optional annotated git tag (Git tag, not "Tags:" in message)
  if prompt_yn "Gitのタグ (git tag -a) も作りますか？" "N"; then
    local tname
    tname="$(prompt "タグ名" "例: v0.3.0")"
    local tmsg
    tmsg="$(prompt "タグの説明" "release")"
    git tag -a "$tname" -m "$tmsg"
    git push origin "$tname"
    echo "🏷️ タグ作成＆push: $tname"
  fi
}

do_merge() {
  need_git_repo

  echo ""
  echo "🔀 Merge を開始します。"
  echo "現在のブランチ(マージ先): $(current_branch)"
  echo ""

  ensure_clean_or_confirm

  git fetch origin --prune

  echo ""
  echo "📚 ローカルブランチ:"
  git branch --format="%(refname:short)" | sed 's/^/  - /'

  echo ""
  echo "🌐 リモート(origin)ブランチ:"
  git branch -r --format="%(refname:short)" | sed 's/^/  - /'

  echo ""
  local from
  from="$(prompt "どのブランチを取り込みますか？ (例: dev)" "dev")"

  # try local first, fallback to origin/<from>
  if git show-ref --verify --quiet "refs/heads/$from"; then
    echo "➡️ 取り込み元: $from (local)"
    git merge --no-ff "$from"
  elif git show-ref --verify --quiet "refs/remotes/origin/$from"; then
    echo "➡️ 取り込み元: origin/$from (remote)"
    git merge --no-ff "origin/$from"
  else
    die "ブランチが見つかりません: $from"
  fi

  echo ""
  echo "✅ merge 完了。"

  if prompt_yn "マージ先ブランチを push しますか？" "Y"; then
    local branch
    branch="$(current_branch)"
    if git rev-parse --abbrev-ref --symbolic-full-name "@{u}" >/dev/null 2>&1; then
      git push
    else
      git push -u origin "$branch"
    fi
    echo "🚀 push 完了"
  fi
}

do_pull() {
  need_git_repo

  echo ""
  echo "⬇️ Pull を開始します。"
  echo "対象ブランチ: $(current_branch)"
  echo ""

  ensure_clean_or_confirm

  echo "（安全のため --ff-only を使います。履歴が分岐していると停止します）"
  if git pull --ff-only; then
    echo "✅ pull 完了"
  else
    echo ""
    echo "⚠️ fast-forward できませんでした。"
    echo "   対応案:"
    echo "   1) git fetch origin"
    echo "   2) git log --oneline --decorate --graph --all | head"
    echo "   3) 必要なら: git merge origin/$(current_branch)  または git rebase origin/$(current_branch)"
    exit 1
  fi
}

# ---------- main ----------
main() {
  need_git_repo

  echo ""
  echo "🧰 Git Assist"
  echo "Repo: $(git rev-parse --show-toplevel)"
  echo "Branch: $(current_branch)"

  local op
  op="$(select_one "やりたいことを教えて下さい。" "コミット&プッシュ" "マージ" "プル" | tail -n 1 | tr -d '\r')"

  case "$op" in
    "コミット&プッシュ") do_commit_and_push ;;
    "マージ") do_merge ;;
    "プル") do_pull ;;
    *) die "不明な操作です: $op" ;;
  esac
}

main "$@"