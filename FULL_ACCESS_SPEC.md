# Astra Sathi Full Access / Owner Command Mode

## মূল আচরণ

ব্যবহারকারীর বাংলা voice command execution request হিসেবে গণ্য হবে। প্রয়োজনীয় permission সক্রিয় থাকলে Astra Sathi নিজেই কাজ করবে। Permission বাকি থাকলে সংশ্লিষ্ট setup screen খুলবে এবং কী দরকার তা বাংলায় জানাবে। কোনো action ব্যর্থ হলে সফল হয়েছে বলে দাবি করবে না।

## Access layers

1. **Runtime access:** microphone, contacts, camera/torch ও notifications।
2. **Screen control:** active screen পড়া, text/description অনুযায়ী control খোঁজা, click, focused field-এ type, scroll, swipe, Home/Back/Recents, notification/Quick Settings, lock ও screenshot।
3. **Notification control:** read, open, dismiss ও supported RemoteInput reply।
4. **Special access:** overlay, usage access, modify system settings, exact alarms, Do Not Disturb ও battery optimization exclusion।
5. **Voice foreground mode:** “হ্যালো সাথী” phrase, বাংলা command, voice confirmation state এবং persistent visible notification।
6. **Personal Memory:** encrypted facts/preferences/history/tasks, due reminder, follow-up reminder, completion tracking ও reboot recovery।
7. **Teach & Adapt:** explicit recording session, actionable-control numbering, privacy-filtered app/click/type/scroll capture, encrypted Routine save ও fuzzy label replay।
8. **Communication control:** package-filtered social notifications, approved WhatsApp send, Android Default Phone role, caller announcement ও active-call controls।
9. **Life Context:** opt-in encrypted fuel/distance/mileage, commitment, medicine ও symptom timeline; proactive reminders এবং transparent estimate/confidence।

## Security contract

- User command ছাড়া autonomous screen action শুরু হবে না।
- Message/call, sensitive type/click, security settings, uninstall ও irreversible action confirmation চাইবে।
- OTP, PIN, password, CVV, card number বা UPI PIN Personal Memory-তে রাখা হবে না।
- Teach Mode password/protected field বা payment/trading final action record করবে না; recording state notification-এ দৃশ্যমান থাকবে।
- Android credential/biometric, banking protection, protected confirmation, app sandbox বা OS security bypass করা হবে না।
- Root ব্যবহার Full Access-এর সাধারণ requirement নয় এবং release build root চাইবে না।
- Call control শুধু user-granted Default Phone role-এর `InCallService` ব্যবহার করবে; phone UI বা emergency-call protection bypass করবে না।
- Social message read/reply Notification Access ও supported RemoteInput-এর সীমার মধ্যে থাকবে; private app database scrape করা হবে না।

## Financial Action Mode

Share trading, mutual fund, banking বা UPI generic Accessibility click sequence দিয়ে execute করা হবে না। প্রতি provider/broker-এর official API adapter লাগবে। Flow:

1. User exact order বলবেন: broker/account, symbol, Buy/Sell, quantity, Market/Limit, price, product ও validity।
2. Live quote ও account/risk data broker API থেকে নেওয়া হবে।
3. App সম্পূর্ণ order preview ও estimated charges পড়ে শোনাবে।
4. User transaction-specific biometric/device credential confirmation দেবেন।
5. Signed request broker API-তে যাবে; returned order ID ও status immutable audit log-এ থাকবে।
6. Reject/partial fill/cancel/error হলে exact broker response বাংলায় জানানো হবে।

Broker API token Android Keystore-protected storage-এ থাকবে; trading password/OTP voice memory-তে থাকবে না। API order tagging, static IP, strategy registration বা অন্য exchange/broker requirement প্রযোজ্য হলে adapter সেই requirement enforce করবে।
