# Astra Sathi Multi-Step Workflow Engine

## Voice format

“তারপর”, “এরপর”, “তার পরে” বা “এর পরে” দিয়ে একাধিক atomic command জোড়া যায়। উদাহরণ:

> WhatsApp খোলো, তারপর Search-এ চাপ দাও, তারপর লিখো রাহুল, তারপর রাহুলে চাপ দাও।

## Supported step types

- Atomic command: app/settings open, click, type, scroll/swipe, media/settings action।
- Delay: “২ সেকেন্ড অপেক্ষা করো”।
- Screen condition: “Success লেখা আসা পর্যন্ত অপেক্ষা করো”।
- Verification: click/type API result, visible-text polling এবং timeout।

একসঙ্গে একটি workflow চলে। নতুন workflow শুরু হলে আগেরটি cancel হয়। প্রতিটি ধাপের status UI/foreground notification-এ দেখা যায়। Failure হলে runner পরের ধাপে যায় না।

## Routine

শুধু সম্পূর্ণ সফল workflow Routine হিসেবে save করা যায়। Personal Memory চালু থাকতে হবে। Routine source encrypted memory-তে থাকে। Voice commands:

- “এই কাজটা সকাল Routine হিসেবে মনে রাখো”।
- “সকাল Routine চালাও”।
- “আমার Routine দেখাও”।

Sensitive ধাপযুক্ত Routine প্রতিবার execution-এর আগে confirmation নেয়। Financial order সাধারণ workflow বা Routine-এর মধ্যে চালানো নিষিদ্ধ; এটি Financial Action Mode-এ যায়।

## Teach & Adapt Mode

Voice commands:

- “রেকর্ড শুরু করো” — explicit recording session শুরু।
- “রেকর্ড বন্ধ করো সকাল নামে” — শেখা ধাপ encrypted Routine হিসেবে save।
- “রেকর্ড বাতিল করো” — session-এর সব অস্থায়ী ধাপ discard।
- “স্ক্রিনের বোতামগুলো দেখাও” — সর্বোচ্চ ১৫টি current actionable control নম্বরসহ পড়া।
- “৩ নম্বরে চাপ দাও” — একই screen snapshot-এর ৩ নম্বর control click।

Recorder app change, labeled click, non-sensitive text change ও scroll direction capture করে; একই text field-এর দ্রুত পরিবর্তন সর্বশেষ পূর্ণ value দিয়ে replace হয়। সর্বোচ্চ ৬০ ধাপ নেওয়া হয়। Password/protected node, OTP/PIN/card/UPI credential এবং payment/trading final action বাদ যায়। অন্তত ২টি নিরাপদ ধাপ না থাকলে Routine save হয় না। Replay-তে exact label না পেলেও Bengali/English normalized fuzzy score confidence threshold পার হলে control নির্বাচন হয়।

## Financial boundary

Trade voice order parse ও preview হয়, কিন্তু `BrokerRegistry`-তে official connected adapter না থাকলে submit button disabled থাকে। নতুন broker যোগ করতে `BrokerGateway` implementation, official authentication, exchange/broker compliance, immutable order audit এবং transaction-specific device authentication আবশ্যক।
