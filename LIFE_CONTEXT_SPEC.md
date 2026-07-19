# Astra Sathi JARVIS Life Context v0.6

## উদ্দেশ্য

ব্যবহারকারীর বিচ্ছিন্ন voice command-কে সময়-সংযুক্ত জীবনঘটনা হিসেবে রাখা, যাতে Astra Sathi গাড়ি, দেওয়া কথা এবং স্বাস্থ্য-লগ মিলিয়ে প্রাসঙ্গিক বাংলা সহায়তা দিতে পারে।

## Observe → Remember → Reason → Remind

1. ব্যবহারকারী বাংলা voice/text-এ একটি ঘটনা বলেন।
2. `LifeContextParser` intent, subject, amount, relative day/time এবং confidence আলাদা করে।
3. Personal Memory opt-in থাকলেই `LifeContextRepository` Android Keystore AES-GCM encryption-এ event রাখে।
4. `LifeContextEngine` known fact এবং estimate আলাদা করে বাংলা result দেয়।
5. Commitment/refuel task হলে existing reminder engine exact/approximate alarm schedule করে এবং অসম্পূর্ণ থাকলে follow-up দেয়।

## Vehicle Context

- Fuel fill event: date/time এবং optional litre quantity।
- Distance event: user-reported run in kilometre।
- Mileage event: kilometre per litre।
- Estimate: `remaining litre = last fill litre − distance since fill / mileage`।
- Fuel quantity বা mileage না থাকলে app estimate না করে missing input জানায়।
- এই version-এ GPS trip meter, fuel sensor বা Bluetooth OBD data নেই।

## Commitment Context

- “আমি রাহুলকে বলেছি কালকে রিপোর্ট দিয়ে দেবো” থেকে person, task এবং day/time parse।
- সময় বলা না থাকলে আগামী দিনের commitment সকাল ৯টায়; আজকের unspecified commitment দুই ঘণ্টা পরে remind করে এবং response-এ assumed time জানায়।
- Connected call শেষে optional visible prompt থাকে। Call audio record/transcribe করা হয় না।
- User “শেষ প্রতিশ্রুতি সম্পন্ন” বলে latest commitment complete করতে পারেন।

## Health Context

- Medicine intake এবং user-reported symptom time-stamped event হিসেবে থাকে।
- Status শুধু latest reported facts summarize করে এবং change জানতে চায়।
- App diagnosis, prescription, dosage calculation বা emergency monitoring করে না।

## বাংলা command

- “তিন দিন আগে গাড়িতে ৫ লিটার তেল ভরেছি”
- “গাড়ি লিটারে ১৫ কিলোমিটার মাইলেজ দেয়”
- “গাড়ি ১৭৮ কিলোমিটার চালিয়েছি”
- “গাড়ির তেলের অবস্থা বলো”
- “আজ গাড়িতে তেল ভরতে হবে”
- “আমি রাহুলকে বলেছি কালকে রিপোর্ট দিয়ে দেবো”
- “কী কথা দিয়েছি”
- “আজ দুপুর ২টায় পেট ব্যথার ওষুধ খেয়েছি”
- “আমার পেটে ব্যথা হচ্ছে”
- “আমার বর্তমান পরিস্থিতি কেমন”

## সীমা ও নিরাপত্তা

- Personal Memory বন্ধ থাকলে কোনো Life Context event save হয় না।
- OTP/PIN/password/CVV/card/UPI credential filter বহাল থাকে।
- সব memory clear করলে Life Context store-ও clear হয়।
- Confidence কম হলে dashboard-এ user confirmation দরকার বলে দেখায়।
- কথোপকথনের content অনুমতি ছাড়া capture করা হয় না।
