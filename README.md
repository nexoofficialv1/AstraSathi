# Astra Sathi — আপনার ব্যক্তিগত বাংলা সহকারী

এটি একটি Native Android personal assistant। বাংলায় কথা বলে বা লিখে ফোনের কাজ চালানো যায়। Full Access Mode-এ ব্যবহারকারী স্বেচ্ছায় অনুমতি দিলে Accessibility-based screen control, notification control, settings control, encrypted memory এবং opt-in “হ্যালো সাথী” foreground listening কাজ করে। কোনো AI API key ছাড়াই মূল command engine কাজ করে।

## বর্তমানে কার্যকর

- বাংলা speech-to-text এবং বাংলা text-to-speech
- যোগাযোগের নাম/নম্বর ধরে Call dialer, SMS ও WhatsApp message প্রস্তুত
- বাংলা সময় বুঝে Alarm তৈরি
- আজ/আগামীকাল/পরশু ধরে Calendar reminder তৈরি
- ফোনে ব্যক্তিগত নোট সংরক্ষণ ও সাম্প্রতিক নোট দেখানো
- WhatsApp, Maps, Gmail, YouTube, Calendar, Camera ও Settings খোলা
- Google search, আবহাওয়া search এবং Maps navigation
- Torch চালু/বন্ধ
- “হ্যালো সাথী” wake phrase শনাক্ত করা—অ্যাপের listening screen-এ
- ঝুঁকিপূর্ণ/বাহ্যিক কাজের আগে বাংলা confirmation dialog
- Accessibility দিয়ে screen-এর লেখা পড়া, দৃশ্যমান control click, focused field-এ লেখা, scroll/swipe
- Home, Back, Recents, notification panel, Quick Settings, lock ও screenshot command
- Notification পড়া, খোলা, dismiss এবং supported notification quick reply
- Installed app-এর label ধরে যেকোনো launcher app খোলা ও app info/uninstall confirmation খোলা
- Brightness, auto-rotate, screen timeout, volume, ringer mode ও media control
- Wi-Fi/Bluetooth/Location/Security/Privacy/Display/Sound settings navigation
- Personal Memory: Android Keystore AES-GCM encrypted on-device memory
- Due task reminder; অসম্পূর্ণ থাকলে ৩০ মিনিট অন্তর সর্বোচ্চ ৩টি follow-up reminder
- Reboot/app update-এর পর pending reminders পুনরায় schedule
- Opt-in foreground “হ্যালো সাথী” mode; notification ও microphone indicator সবসময় দৃশ্যমান
- Multi-step workflow: “তারপর/এরপর” দিয়ে app open, click, type, wait, scroll ইত্যাদি ধারাবাহিকভাবে চালানো
- Step verification, timeout, failure step report ও একসঙ্গে একটি workflow lock
- সফল workflow-কে encrypted Routine হিসেবে নাম দিয়ে save ও voice-এ পুনরায় চালানো
- Teach & Adapt Mode: ব্যবহারকারী কাজটি একবার করে দেখালে app open/click/type/scroll ধাপ privacy filter সহ Routine হিসেবে শেখা
- বর্তমান screen-এর actionable control নম্বরসহ পড়ে শোনানো এবং “৩ নম্বরে চাপ দাও” command
- বাংলা/English button label সামান্য বদলালেও typo-tolerant fuzzy matching
- Teach Mode চলাকালে persistent notification ও সর্বোচ্চ ৬০ ধাপের recording limit
- Facebook/Facebook Lite/Messenger ও WhatsApp/WhatsApp Business বাংলা নাম alias ধরে খোলা
- WhatsApp/Facebook/Messenger-এর package-specific সাম্প্রতিক notification পড়া, খোলা ও supported quick reply
- User confirmation-এর পর WhatsApp chat deep-link এবং exact Send control automation; result notification
- Default Phone role-ভিত্তিক incoming caller announcement, incoming/ongoing call UI ও recent call list
- “কে কল করছে”, “কল ধরো”, “কল কেটে দাও”, speaker এবং mute voice control
- JARVIS Life Context: গাড়িতে তেল ভরা, চলা কিলোমিটার ও mileage সময়সহ encrypted log
- সর্বশেষ fuel fill + distance + mileage থেকে অবশিষ্ট তেলের স্পষ্টভাবে চিহ্নিত আনুমানিক হিসাব
- “আজ গাড়িতে তেল ভরতে হবে” বললে আজকের proactive reminder
- কাউকে দেওয়া কথার ব্যক্তি, কাজ ও সম্ভাব্য সময় ধরে commitment log এবং reminder
- সম্পন্ন call-এর পরে visible follow-up prompt; call audio গোপনে record বা transcribe করা হয় না
- ওষুধ খাওয়ার সময়, বলা উপসর্গ ও পরিবর্তনের health-context log; diagnosis নয়
- এক command-এ vehicle, pending commitment ও health summary: “আমার বর্তমান পরিস্থিতি কেমন”
- আলাদা Life Context dashboard এবং সব তথ্য মুছে দেওয়ার unified control
- Financial Action Preview: symbol, Buy/Sell, quantity, Market/Limit, price ও product parse
- BrokerGateway boundary; official broker adapter না থাকলে real order submission hard-disabled

## উদাহরণ কমান্ড

- “হ্যালো সাথী, মাকে ফোন করো”
- “রাহুলকে মেসেজ পাঠাও আমি দশ মিনিট পরে আসছি”
- “রাহুলকে হোয়াটসঅ্যাপে মেসেজ পাঠাও আমি আসছি”
- “সকাল ৭টায় অ্যালার্ম দাও”
- “আগামীকাল সকাল ১০টায় অফিসের রিমাইন্ডার দাও”
- “নোট লেখো কাল রিপোর্ট জমা দিতে হবে”
- “আমার নোট দেখাও”
- “কালনা থানার রাস্তা দেখাও”
- “কালনার আজকের আবহাওয়া দেখাও”
- “টর্চ জ্বালাও”
- “স্ক্রিনের লেখা পড়ো”
- “পাঠান বোতামে চাপ দাও”
- “স্ক্রিনের বোতামগুলো দেখাও”
- “৩ নম্বরে চাপ দাও”
- “এখানে লিখো আমি আগামীকাল আসব”
- “Brightness ৫০ শতাংশ করো”
- “Security settings খোলো”
- “মনে রেখো প্রতি সোমবার রিপোর্ট দিতে হবে”
- “আমি কী ভুলে গেছি”
- “কাজটা হয়ে গেছে”
- “WhatsApp খোলো, তারপর Search-এ চাপ দাও, তারপর লিখো রাহুল”
- “এই কাজটা সকাল Routine হিসেবে মনে রাখো”
- “সকাল Routine চালাও”
- “রেকর্ড শুরু করো”—কাজটি একবার করে দেখান
- “রেকর্ড বন্ধ করো সকাল নামে”
- “রেকর্ড বাতিল করো”
- “Facebook চালাও”
- “WhatsApp এ মেসেজ দেখাও”
- “WhatsApp-এর মেসেজের উত্তর দাও আমি আসছি”
- “কে কল করছে” / “কল ধরো” / “কল কেটে দাও”
- “কলের স্পিকার চালু” / “কল মিউট করো”
- “তিন দিন আগে গাড়িতে ৫ লিটার তেল ভরেছি”
- “গাড়ি লিটারে ১৫ কিলোমিটার mileage দেয়”
- “এরপর গাড়ি ১৭৮ কিলোমিটার চালিয়েছি”
- “গাড়ির তেলের অবস্থা বলো” / “আজ গাড়িতে তেল ভরতে হবে”
- “আমি রাহুলকে বলেছি কালকে রিপোর্ট দিয়ে দেবো”
- “কী কথা দিয়েছি” / “শেষ প্রতিশ্রুতি সম্পন্ন”
- “আজ দুপুর ২টায় পেট ব্যথার ওষুধ খেয়েছি”
- “আমার পেটে ব্যথা হচ্ছে” / “আমার শরীরের অবস্থা কেমন”
- “আমার বর্তমান পরিস্থিতি কেমন”
- “TCS-এর ১০টা share Market price-এ কিনো”—Financial Preview

## Android Studio-তে চালানো

1. Android Studio-তে `AstraSathi` folder খুলুন।
2. JDK 17, Android SDK 35 এবং Gradle sync ব্যবহার করুন।
3. একটি Android 8.0+ ফোন USB debugging দিয়ে যুক্ত করুন।
4. Run চাপুন এবং প্রথম ব্যবহারে Microphone/Contacts/Camera permission অনুমোদন করুন।

Release APK বানাতে Android Studio থেকে **Build > Generate Signed App Bundle or APK > APK** ব্যবহার করুন। Application ID: `com.astratechnologies.astrasathi`।

## GitHub Actions দিয়ে APK

Project-এ `.github/workflows/android-apk.yml` রয়েছে। GitHub-এর `main`/`master` branch-এ push করলে JDK 17, Gradle 8.9 ও Android SDK 35 দিয়ে tests চালিয়ে installable debug APK artifact তৈরি হবে। Gradle wrapper না থাকলেও workflow `setup-gradle` action দিয়ে নির্দিষ্ট Gradle version ব্যবহার করে।

ফোনের Termux থেকে source push এবং GitHub Actions থেকে APK download করার বাংলা ধাপগুলো `TERMUX_GITHUB_BUILD.md`-এ রয়েছে। দ্রুত push করার জন্য project folder থেকে চালান:

```bash
bash scripts/termux_push_github.sh
```

Debug APK testing-এর জন্য। Play Store/production release-এর আগে স্থায়ী private release keystore, GitHub Secrets এবং release signing configuration লাগবে। Keystore/PAT/token repository-তে commit করা যাবে না।

## নিরাপত্তা নীতি

- `ACTION_DIAL` ব্যবহার করা হয়েছে; অ্যাপ নিজে নীরবে কল করে না।
- SMS/WhatsApp/Calendar/Alarm পরবর্তী অফিসিয়াল app screen-এ প্রস্তুত হয়; ব্যবহারকারী চূড়ান্ত নিয়ন্ত্রণে থাকেন।
- নোট শুধু ফোনের private app storage-এ থাকে।
- OTP, PIN, password, CVV, card number এবং UPI PIN Personal Memory-তে রাখা হয় না।
- Teach Mode password/protected field-এর click বা typed value, OTP/PIN এবং payment/trade final action রেকর্ড করে না।
- Banking/UPI/stock trading-এর মতো financial execution generic screen-click automation দিয়ে করা হয় না; official provider API, order preview ও transaction-specific authentication লাগে।
- Android-এর system credential, biometric, protected confirmation ও app sandbox bypass করা হয় না।
- Call answer/end control কেবল user-selected Default Phone role ও Android Telecom `InCallService` দিয়ে হয়। Emergency call system dialer-এর নিয়ন্ত্রণে থাকে।
- Life Context-এর fuel balance sensor reading নয়। Fuel quantity, user-reported distance ও mileage থাকলেই app একটি আনুমানিক হিসাব দেখায়। Bluetooth OBD/vehicle telemetry এই version-এ যুক্ত নয়।
- Call audio record করা হয় না। একটি call সত্যিই connected হওয়ার পরে শুধু visible post-call prompt দেখানো হয়, যাতে ব্যবহারকারী নিজে দেওয়া কথাটি বলেন বা লেখেন।
- Medicine/symptom log ব্যবহারকারীর বলা তথ্যের timeline; এটি রোগ নির্ণয়, prescription বা emergency monitoring নয়।
- WhatsApp/Facebook message history server বা app database থেকে গোপনে নেওয়া হয় না; Notification Access-এ পাওয়া notification এবং user-visible screen-ই ব্যবহৃত হয়।

## পরবর্তী নির্ধারিত ধাপ

- পুলিশি report/letter drafting module
- encrypted backup ও device-to-device sync
- user-approved AI conversation endpoint
- optional Bluetooth OBD এবং user-approved Health Connect adapter
- broker-specific, regulation-compliant Financial Action adapters
- Bengali contact disambiguation ও custom command training

© Astra Technologies
