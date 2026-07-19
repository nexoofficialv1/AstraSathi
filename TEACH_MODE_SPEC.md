# Astra Sathi Teach & Adapt Mode

## লক্ষ্য

ব্যবহারকারী কোনো mobile task একবার নিজে করে দেখাবেন। Astra Sathi সেই demonstration-এর নিরাপদ, পুনরায় চালানোযোগ্য ধাপগুলো encrypted Routine হিসেবে শিখবে। এটি screen video recorder নয়; Accessibility event থেকে সীমিত structured action সংগ্রহ করে।

## Session lifecycle

1. “রেকর্ড শুরু করো” বললে Screen Control ও Personal Memory সক্রিয় আছে কি না যাচাই হয়।
2. Persistent notification-এ recording state ও captured step count দেখা যায়।
3. App open, labeled click, non-sensitive type এবং scroll সর্বোচ্চ ৬০ ধাপ পর্যন্ত capture হয়।
4. “রেকর্ড বন্ধ করো সকাল নামে” বললে অন্তত ২টি নিরাপদ ধাপ encrypted Routine হিসেবে save হয়।
5. “রেকর্ড বাতিল করো” বললে temporary steps মুছে যায়।

## Privacy exclusions

- Password node বা protected input field।
- OTP, PIN, passcode, CVV/CVC, card number, UPI PIN বা security code।
- Payment/money transfer এবং share-trading final order action।
- Astra Sathi-এর নিজের UI, Android System UI ও launcher navigation noise।

বাদ যাওয়া sensitive value log, notification বা Routine-এর কোনো জায়গায় রাখা হয় না।

## Adaptive replay

Click replay exact text, content description এবং resource-id tail তুলনা করে। Exact, prefix, contains, token overlap ও edit-distance score ব্যবহার করা হয়; confidence threshold না পেরোলে runner ভুল control চাপার বদলে সংশ্লিষ্ট ধাপে থামে। Numbered action snapshot অন্য app/screen-এ বদলে গেলে user-কে নতুন করে action list চাইতে বলা হয়।

## সীমা

Teach Mode Android credential/biometric, protected confirmation বা app sandbox bypass করে না। Banking/UPI/share order generic Accessibility Routine দিয়ে submit হয় না; official provider adapter এবং transaction-specific authentication আবশ্যক।
