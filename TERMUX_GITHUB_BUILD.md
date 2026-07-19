# Termux থেকে GitHub-এ push করে Astra Sathi APK বানানো

এই project-এ `.github/workflows/android-apk.yml` প্রস্তুত আছে। GitHub-এ `main` branch push হলেই tests চালিয়ে installable debug APK তৈরি হবে।

## ১. GitHub-এ খালি repository তৈরি করুন

GitHub website/app থেকে **AstraSathi** নামে একটি repository তৈরি করুন। প্রথমবার repository তৈরির সময় README, `.gitignore` বা License যোগ করবেন না—project-এর মধ্যে এগুলো আগে থেকেই আছে।

## ২. Source ZIP ফোনে রাখুন

এই source ZIP-টি ফোনের **Download** folder-এ রাখুন:

`AstraSathi_v0.6.0_GitHub_APK_Build_Source.zip`

## ৩. Termux প্রস্তুত করুন

Termux-এ একটির পর একটি চালান:

```bash
pkg update -y
pkg install git gh unzip -y
termux-setup-storage
mkdir -p "$HOME/astra-sathi-upload"
cd "$HOME/astra-sathi-upload"
unzip -o "/sdcard/Download/AstraSathi_v0.6.0_GitHub_APK_Build_Source.zip"
cd AstraSathi
```

Storage permission চাইলে **Allow** দিন। ZIP-এর নাম ফোনে আলাদা হলে `unzip` command-এ সেই সঠিক নাম লিখুন।

## ৪. GitHub login করুন

```bash
gh auth login --web --git-protocol https
```

GitHub.com নির্বাচন করে browser/device code দিয়ে login সম্পন্ন করুন। Password বা token project file-এর মধ্যে লিখবেন না।

## ৫. Push করুন

```bash
bash scripts/termux_push_github.sh
```

Script যেগুলো চাইবে:

- GitHub username
- Repository name: Enter চাপলে `AstraSathi`
- Commit name
- GitHub email: খালি রাখলেও হবে

সফল হলে শেষে **Push সম্পন্ন** দেখাবে।

## ৬. APK download করুন

1. GitHub repository খুলুন।
2. **Actions → Build Android APK** খুলুন।
3. সর্বশেষ green/successful run খুলুন।
4. নিচের **Artifacts** থেকে `AstraSathi-APK-...` download করুন।
5. Download করা artifact ZIP extract করুন।
6. `AstraSathi-v0.6.0-debug.apk` চাপুন এবং **Install unknown apps** অনুমতি দিয়ে install করুন।

Push-এর পর workflow নিজে শুরু না হলে **Actions → Build Android APK → Run workflow** চাপুন।

## “App not installed” দেখালে

আগে একই package name-এর Astra Sathi অন্য signature দিয়ে install করা থাকলে Android নতুন debug APK গ্রহণ করবে না। পুরোনো app-এর দরকারি local data backup নিয়ে app-টি uninstall করে নতুন APK install করুন। Uninstall করলে app-এর local notes ও Personal Memory মুছে যেতে পারে।

প্রতিটি GitHub runner নতুন debug signing key তৈরি করতে পারে। নিয়মিত update install করার জন্য পরবর্তী ধাপে একটি স্থায়ী release keystore GitHub Secrets-এ রেখে signed release APK build করতে হবে; keystore কখনো repository-তে commit করা যাবে না।

## Build ব্যর্থ হলে

Actions run-এর লাল step খুলে error text বা screenshot পাঠান। Workflow failure হলে সম্ভব হলে `AstraSathi-build-reports-...` artifact-ও তৈরি হবে। সেই error দেখে source ঠিক করে আবার push করা যাবে।
