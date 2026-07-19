# Astra Sathi Communication Control v0.5

## Facebook ও Messenger

- Bengali/English alias থেকে Facebook, Facebook Lite, Messenger বা Messenger Lite launcher package খোলা যায়।
- “Facebook-এর notification দেখাও/খোলো” এবং “Messenger-এর message পড়ো” command package-filtered Notification Access ব্যবহার করে।
- Supported messaging notification-এ RemoteInput থাকলে confirmation-এর পর voice reply পাঠানো যায়।
- Feed, profile, post, menu ও scrolling-এর জন্য existing Screen Control, numbered controls এবং Teach Mode ব্যবহার হয়।

## WhatsApp

- WhatsApp ও WhatsApp Business notification আলাদাভাবে filter হয়। সর্বোচ্চ পাঁচটি recent unique notification পড়ে শোনানো যায়।
- Existing quick-reply action থাকলে confirmation-এর পর reply পাঠানো হয়।
- নতুন recipient-এর message-এ contact resolve → confirmation → `wa.me` chat deep-link → exact Send label polling হয়। Screen Control বা exact label না থাকলে chat প্রস্তুত থাকে এবং user নিজে Send চাপেন।
- WhatsApp encrypted database, deleted message বা notification-এর বাইরে থাকা history access করা হয় না।

## Caller ও Call Control

Call control-এর জন্য user-কে Full Access Setup থেকে Astra Sathi-কে Android Default Phone app নির্বাচন করতে হবে। App `ACTION_DIAL` UI এবং `InCallService` incoming/ongoing UI দেয়।

Supported commands:

- “কে কল করছে” / “কে কল করেছে”।
- “কল ধরো” / “কল কেটে দাও”।
- “কলের স্পিকার চালু/বন্ধ”।
- “কল মিউট করো/আনমিউট করো”।

Incoming call এলে Contacts permission থাকলে local contact name, অন্যথায় number বাংলা TTS-এ ঘোষণা হয়। High-priority call notification-এ Answer/End action থাকে। Recent call list শুধু `READ_CALL_LOG` permission থাকলে দেখানো হয়। Multiple calls-এর মধ্যে ringing call, তারপর active call অগ্রাধিকার পায়।

## Platform boundaries

- Default Dialer role user consent ছাড়া নেওয়া হয় না।
- Emergency call preloaded system dialer পরিচালনা করে।
- Voice answer reliability device-এর microphone/audio-focus policy-এর উপর নির্ভরশীল; visible call notification ও call screen সবসময় fallback।
- Facebook/WhatsApp UI version বদলালে exact Send action না পেলে app সফল দাবি না করে user-কে manual review জানায়।
