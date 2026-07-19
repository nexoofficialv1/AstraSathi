#!/data/data/com.termux/files/usr/bin/bash
set -euo pipefail

if ! command -v git >/dev/null 2>&1; then
  echo "Git পাওয়া যায়নি। আগে চালান: pkg install git"
  exit 1
fi

if [ ! -f "settings.gradle" ] || [ ! -f ".github/workflows/android-apk.yml" ]; then
  echo "AstraSathi project folder-এর ভিতর থেকে script-টি চালান।"
  exit 1
fi

printf "GitHub username: "
read -r github_user
if [ -z "$github_user" ]; then
  echo "GitHub username খালি রাখা যাবে না।"
  exit 1
fi

printf "Repository name [AstraSathi]: "
read -r repository_name
repository_name="${repository_name:-AstraSathi}"

printf "Commit name [Astra Technologies]: "
read -r commit_name
commit_name="${commit_name:-Astra Technologies}"

printf "GitHub email (খালি রাখলে no-reply email): "
read -r commit_email
commit_email="${commit_email:-${github_user}@users.noreply.github.com}"

if ! git rev-parse --is-inside-work-tree >/dev/null 2>&1; then
  git init -b main
else
  git branch -M main
fi

git config user.name "$commit_name"
git config user.email "$commit_email"
remote_url="https://github.com/${github_user}/${repository_name}.git"
if git remote get-url origin >/dev/null 2>&1; then
  git remote set-url origin "$remote_url"
else
  git remote add origin "$remote_url"
fi

git add .
if git diff --cached --quiet; then
  echo "নতুন কোনো পরিবর্তন নেই—বর্তমান commit push করা হবে।"
else
  git commit -m "Add Astra Sathi with GitHub APK build"
fi

echo "Push শুরু হচ্ছে: $remote_url"
echo "Authentication চাইলে আগে 'gh auth login --web --git-protocol https' চালান।"
git push -u origin main

echo "Push সম্পন্ন। GitHub repository-এর Actions tab-এ APK build দেখুন।"
