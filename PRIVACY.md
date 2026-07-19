# Astra Sathi গোপনীয়তা ও অনুমতি

## Microphone

ব্যবহারকারী মাইক্রোফোন button চাপলে বাংলা voice command শনাক্ত করতে ব্যবহৃত হয়। “হ্যালো সাথী” Mode আলাদাভাবে চালু করলে foreground service microphone ব্যবহার করে; তখন Android-এর persistent notification ও microphone indicator দৃশ্যমান থাকে।

## Contacts

“মাকে ফোন করো” ধরনের command-এ নাম থেকে ফোন নম্বর খুঁজতে ব্যবহৃত হয়। Contacts copy বা server-এ upload করা হয় না।

## Camera

শুধু ফোনের torch চালু/বন্ধ করতে প্রয়োজন। ছবি ধারণ বা upload করা হয় না।

## Local notes

ব্যক্তিগত নোট Android private app storage-এ থাকে। App uninstall করলে operating system এই data মুছে দিতে পারে।

## Personal Memory

ব্যবহারকারী নিজে চালু করলে বলা তথ্য, পছন্দ, command history ও অসম্পূর্ণ কাজ Android private storage-এ AES-GCM encryption সহ রাখা হয়; encryption key Android Keystore-এ থাকে। OTP, PIN, password, CVV, card number ও UPI PIN filter করে সংরক্ষণ করা হয় না। ব্যবহারকারী Memory বন্ধ বা সম্পূর্ণ মুছতে পারেন।

## JARVIS Life Context

Personal Memory চালু থাকলে vehicle fuel/distance/mileage, user-confirmed commitment, medicine এবং symptom event আলাদা AES-GCM encrypted on-device store-এ সময়, উৎস ও confidence-সহ রাখা হয়। “সব Personal Memory মুছুন” action এই Life Context-ও মুছে দেয়। কোনো Life Context data server-এ upload হয় না।

Fuel status কোনো vehicle sensor reading নয়; ব্যবহারকারীর দেওয়া fuel quantity, distance ও mileage থেকে estimate করা হয় এবং UI-তে আনুমানিক বলে দেখানো হয়। Medicine/symptom timeline diagnosis বা medical advice নয়।

## Accessibility / Screen Control

Full Access Mode চালু করলে service বর্তমান screen-এর accessibility text পড়তে এবং user-requested click/type/scroll/global navigation করতে পারে। এটি খুব শক্তিশালী access; system Settings থেকে যেকোনো সময় বন্ধ করা যায়। Protected Android security বা app sandbox bypass করা হয় না।

## Teach & Adapt Mode

ব্যবহারকারী স্পষ্টভাবে “রেকর্ড শুরু করো” বললেই Teach Mode Accessibility event থেকে app open, button click, non-sensitive text entry ও scroll ধাপ সংগ্রহ করে। Recording চলার সময় persistent notification থাকে এবং user “রেকর্ড বন্ধ” বা “রেকর্ড বাতিল” করতে পারেন। শেখানো Routine encrypted Personal Memory-তে থাকে। Password field, OTP, PIN, password, CVV/card/UPI credential এবং protected payment/trade final action record করা হয় না। কোনো screen image বা recording server-এ upload করা হয় না।

## Notification access

চালু করলে সর্বশেষ notification পড়া, খোলা, dismiss এবং supported quick reply করা যায়। Notification content server-এ upload করা হয় না।

WhatsApp, WhatsApp Business, Facebook, Facebook Lite এবং Messenger command package অনুযায়ী notification filter করে। শুধুমাত্র Notification Access চালু থাকার সময় Android যে notification দেয় সেটিই পড়া বা RemoteInput reply করা যায়; app-এর encrypted chat database বা মুছে যাওয়া message access করা হয় না।

## Phone, Call Log ও Default Phone role

ব্যবহারকারী Astra Sathi-কে Android-এর Default Phone app হিসেবে নির্বাচন করলে `InCallService` incoming/ongoing call পরিচালনা করে। Contacts permission থাকলে incoming number থেকে local contact name বের করে on-device বাংলা Text-to-Speech-এ ঘোষণা করা হয়। Call answer/end, speaker ও mute কেবল active Telecom call-এ user command বা visible call button থেকে চলে। Recent caller দেখাতে Call Log permission ব্যবহৃত হয়। Contact/call history server-এ upload হয় না। Emergency call Android-এর preloaded dialer পরিচালনা করে।

Connected call শেষ হলে Personal Memory চালু থাকা অবস্থায় একটি visible follow-up notification জিজ্ঞেস করতে পারে ব্যবহারকারী কোনো কাজের কথা দিয়েছেন কি না। App call audio record, intercept বা secretly transcribe করে না। ব্যবহারকারী prompt-এ tap করে নিজে summary বললে তবেই commitment রাখা হয়।

## Approved WhatsApp send

নতুন WhatsApp message-এর recipient ও text confirmation-এর পর official `wa.me` deep link দিয়ে WhatsApp/WhatsApp Business chat খোলা হয়। Screen Control সক্রিয় থাকলে exact “Send/পাঠান” accessibility label পাওয়া গেলেই সেটি চাপা হয়; fuzzy “Send money” বা payment control চাপা হয় না। Success/failure notification-এ জানানো হয়।

## Sensitive actions

Call, SMS, WhatsApp, alarm ও reminder-এর আগে confirmation দেখানো হয় এবং চূড়ান্ত কাজ সংশ্লিষ্ট system/app screen-এ সম্পন্ন হয়।

Financial order, security credential change, app uninstall, sensitive text entry ও protected button action-এর ক্ষেত্রে blanket permission-কে final transaction authorization ধরা হয় না। Transaction/action-specific confirmation প্রয়োজন।
