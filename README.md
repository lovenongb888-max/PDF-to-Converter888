# PDF to Converter888

แอพ Android เปิดไฟล์ PDF และแปลงเป็นรูปภาพ **PNG / JPG** เลือกคุณภาพได้
ผู้พัฒนา: **Chujai888**

## ฟีเจอร์
- เปิดไฟล์ `.pdf` เป็นแอพหลัก อ่านแบบ **เลื่อนขึ้นลงต่อเนื่องทุกหน้า** เต็มจอ ไม่มีแถบบังหน้ากระดาษ
- แตะหน้าจอ 1 ครั้งเพื่อแสดงเมนูลอย (ซ่อนเองใน 4 วินาที): ซูมเข้า/ออก, แปลงไฟล์, ไปยังหน้า, สไลเดอร์เลื่อนหน้า, ปิดแอพ
- **ค้นหาข้อความในเอกสาร** (เมนู ⋮ → ค้นหาในเอกสาร): พิมพ์คำแล้วกดค้นหา ไฮไลต์คำที่เจอบนหน้า กดลูกศรขึ้น/ลงเพื่อไปผลลัพธ์ก่อนหน้า/ถัดไป ค้นในเครื่องเท่านั้น (ไฟล์สแกนที่เป็นภาพล้วนจะค้นไม่ได้)
- ซูม: บีบสองนิ้ว / แตะสองครั้ง / ปุ่ม − +
- โหมดอ่านสบายตา: ปกติ · ถนอมสายตา · กลางคืน (เมนู ⋮)
- แปลงเป็น PNG / JPG — เลือกความละเอียด (72 × 1.5 / 2 / 3 dpi) และคุณภาพ JPG (40–100%)
- แปลงหน้าเดียวหรือทุกหน้า บันทึกที่ `Pictures/PDFtoConverter888`
- เปิด PDF ที่มีรหัสผ่านได้ (ผู้ใช้กรอกเอง แอพไม่จดจำ)
- ไม่มีล็อกอิน ไม่เก็บ/ไม่ส่งข้อมูลผู้ใช้ ไฟล์ประมวลผลในเครื่องเท่านั้น
- ปุ่มปิดแอพมุมขวาบน → ถาม "ปิดแอพและล้างแคช?" → ลบแคชแล้วปิดแอพ
- แจ้งเตือนอัปเดตอัตโนมัติเมื่อมี Release ใหม่บน GitHub
  (ตรวจแค่เลขเวอร์ชันล่าสุดจาก GitHub ไม่ส่งข้อมูลใดๆ ของผู้ใช้)

## สร้างไฟล์ .apk (ผ่าน GitHub Actions)
1. อัปโหลดโปรเจกต์นี้ขึ้น `https://github.com/lovenongb888-max/PDF-to-Converter888`
2. ไปที่แท็บ **Actions** → workflow **Build APK** จะรันเองทุกครั้งที่ push เข้า `main`
   (หรือกด **Run workflow** เอง) แล้วดาวน์โหลดไฟล์ `PDF-to-Converter888.apk` จาก Artifacts
3. ติดตั้งบนมือถือ (เปิดอนุญาตติดตั้งจากแหล่งที่ไม่รู้จัก)

### ออก Release เพื่อให้แอพแจ้งอัปเดต
```bash
git tag v1.0.1
git push origin v1.0.1
```
workflow จะ build และแนบ APK ลง Release อัตโนมัติ แอพเวอร์ชันเก่าจะเห็นแจ้งเตือนอัปเดต

### เซ็นแอพด้วยกุญแจของคุณเอง (แนะนำ)
ถ้าไม่ตั้งค่า APK จะเซ็นด้วย debug key ซึ่งเปลี่ยนทุกครั้ง ทำให้ติดตั้งทับเวอร์ชันเก่าไม่ได้
สร้างกุญแจครั้งเดียว:
```bash
keytool -genkeypair -v -keystore release.jks -alias pdf888 -keyalg RSA -keysize 2048 -validity 10000
base64 -w0 release.jks   # คัดลอกผลลัพธ์
```
แล้วเพิ่ม Secrets ใน GitHub (Settings → Secrets and variables → Actions):
`KEYSTORE_BASE64`, `KEYSTORE_PASSWORD`, `KEY_ALIAS`, `KEY_PASSWORD`
และ **เก็บไฟล์ `release.jks` ไว้ให้ดี ห้ามอัปโหลดขึ้น GitHub**

## วิธีอัปเดตโค้ดบน GitHub
**ทางเว็บ (ง่ายสุด)**
1. แตกไฟล์ zip ใหม่ในเครื่อง
2. เปิด `github.com/lovenongb888-max/PDF-to-Converter888` → **Add file → Upload files**
3. ลากไฟล์/โฟลเดอร์ที่แตกแล้วเข้าไป (ชื่อเดิมจะถูกแทนที่) → **Commit changes**
4. แท็บ **Actions** จะ build APK ให้เอง → ดาวน์โหลดจาก Artifacts

**ทาง git**
```bash
git clone https://github.com/lovenongb888-max/PDF-to-Converter888.git
# คัดลอกไฟล์ใหม่ทับ แล้ว
git add .
git commit -m "Update: continuous reader"
git push
```

**ปล่อยเวอร์ชันใหม่ให้แอพแจ้งอัปเดต**
```bash
git tag v1.2.0
git push origin v1.2.0
```
(หรือบนเว็บ: Releases → Draft a new release → ตั้ง tag `v1.2.0` → Publish)

## Build ในเครื่อง
ต้องมี JDK 17, Android SDK 34 และ Gradle 8.9+
```bash
gradle assembleRelease
```

## ข้อกำหนด
Android 10 (API 29) ขึ้นไป
