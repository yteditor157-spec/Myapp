# SMS READER – সম্পূর্ণ প্রজেক্ট (Android Studio)

Package: `com.ishan.sms`

## কিভাবে খুলবেন

1. Android Studio খুলুন
2. **File → Open** → এই `SMS_Reader_Project` ফোল্ডার সিলেক্ট করুন
3. Gradle Sync হতে দিন (ইন্টারনেট লাগবে)
4. Run / Debug বাটন দিয়ে ফোনে চালান অথবা **Build → Generate Signed Bundle / APK**

> নোট: প্রথমবার খুললে Android SDK path সেট করতে হতে পারে।  
> `local.properties` ফাইলে `sdk.dir=...` লাইন আনকমেন্ট করে আপনার SDK পাথ দিন।

## গুরুত্বপূর্ণ সেটিংস

### `app/build.gradle.kts`
- minSdk = 24
- compileSdk / targetSdk = 34
- Release-এ minify + shrinkResources চালু আছে

### Dependencies (ইতিমধ্যে আছে)
```
androidx.appcompat:appcompat:1.7.0
com.google.android.material:material:1.12.0
androidx.recyclerview:recyclerview:1.3.2
androidx.core:core-ktx:1.13.1
androidx.activity:activity-ktx:1.9.0
```

### `Api.kt`-এ DB_URL
আপনার Firebase Realtime Database URL দিয়ে মিলিয়ে নিন।

## "Powered by Ishan" সুরক্ষা
- লেখা AES-256 এনক্রিপ্ট করা (`Brand.kt`), চাবি প্যাকেজ নাম থেকে তৈরি।
- `BrandFooterView` নিজে লেখাটি আঁকে।
- লেখা পরিবর্তন/মুছে ফেললে অ্যাপ বন্ধ হয়।
- **রিলিজের আগে:** নিজের keystore দিয়ে সাইন করুন →  
  `keytool -list -v -keystore your.jks` থেকে SHA256 (কোলন ছাড়া, ছোট হাতের) নিয়ে `Brand.kt`-এর `CERT = ""`-এ বসান।

## পারমিশন
অ্যাপে ঢোকার পর "সব পারমিশন একসাথে দিন" বাটন দিয়ে SMS, Notification, Battery optimization ইত্যাদি দিন।
