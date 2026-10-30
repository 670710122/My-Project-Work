# Java Multithread File Transfer

โปรเจกต์จำลองการรับส่งไฟล์ระหว่าง Server และ Client ผ่าน Socket โดยเปรียบเทียบการทำงานของ Traditional I/O และ Java NIO รวมถึงทดสอบการดาวน์โหลดด้วย Worker 1 และ 10 Threads

## 1. Project Structure

```text
project2final/
│
├── t_Server.java          # Traditional I/O Server
├── t_Client.java          # Traditional I/O Client
├── t_ServerNIO.java       # NIO Server
├── t_ClientNIO.java       # NIO Client
│
├── test.txt               # ไฟล์ทดสอบ
├── testja.txt             # ไฟล์ทดสอบ
│
└── README.md
```

ไฟล์ `.class` จะถูกสร้างหลัง Compile และไฟล์ `downloaded_...` จะเป็นไฟล์ผลลัพธ์จากการดาวน์โหลด

## 2. Requirements

- Java Development Kit (JDK)
- Visual Studio Code
- Extension Pack for Java (สำหรับการรันผ่าน VS Code)
- PowerShell หรือ Terminal

ตรวจสอบเวอร์ชัน Java:

```powershell
java -version
javac -version
```

## 3. Compile

เปิด PowerShell ใน VS Code แล้วเข้าโฟลเดอร์โปรเจกต์

```powershell
cd "D:\project2final"
```

Compile ไฟล์ Java ทั้งหมด

```powershell
javac -encoding UTF-8 *.java
//เพราะเครื่องนี้อ่านคอมเมนต์ภาษาไทยไม่ได้
```

ตรวจสอบไฟล์ `.class`

```powershell
dir *.class
```

ไฟล์ที่ควรพบ ได้แก่

```text
t_Server.class
t_Client.class
t_ServerNIO.class
t_ClientNIO.class
```

## 4. Run Traditional I/O

Traditional I/O ใช้ Port `5000`

### Terminal 1: Server

```powershell
java t_Server
```

เปิด Server ให้พร้อมรับคำขอจาก Client และทำตามข้อความที่โปรแกรมแสดง เช่น การระบุไฟล์ที่ต้องการให้บริการ

### Terminal 2: Client

เปิด Terminal ใหม่ โดยไม่ปิด Server

```powershell
java t_Client
```

จากนั้นใช้งานเมนูของ Client ตามที่โปรแกรมแสดง

## 5. Run Java NIO

NIO ใช้ Port `5001`

### Terminal 1: Server NIO

```powershell
java t_ServerNIO
```

### Terminal 2: Client NIO

```powershell
java t_ClientNIO
```

หากโปรแกรมรองรับการเลือกจำนวน Worker ให้เลือก `1` หรือ `10` ตามที่ต้องการทดลอง

## 6. Commands

โปรโตคอลสำหรับการรับส่งไฟล์ประกอบด้วย

| Command | Description |
|---|---|
| `LIST` | ขอข้อมูลไฟล์ที่ Server ให้บริการ |
| `INFO` | ขอขนาดไฟล์ |
| `GET` | ขอข้อมูลไฟล์ตาม Offset และ Length |
| `DOWNLOAD` | ดำเนินการดาวน์โหลดผ่าน Client หากมีเมนูรองรับ |
| `EXIT` | ออกจากโปรแกรม Client หากมีเมนูรองรับ |

**หมายเหตุ:** `GET` ใช้ส่งคำขอข้อมูลเป็นช่วงผ่านโปรโตคอล ส่วนคำสั่งที่ผู้ใช้พิมพ์ได้จริงขึ้นอยู่กับเมนูใน Client

## 7. Testing

ทดลองเปรียบเทียบการดาวน์โหลดทั้งหมด 4 กรณี

| Test | Method | Workers |
|---|---|---|
| 1 | Traditional I/O | 1 |
| 2 | Traditional I/O | 10 |
| 3 | Java NIO | 1 |
| 4 | Java NIO | 10 |

ข้อมูลที่ควรบันทึกในการทดลอง ได้แก่

- เวลาที่ใช้ดาวน์โหลด (Seconds)
- Throughput (MB/s)
- ขนาดไฟล์ต้นฉบับและไฟล์ดาวน์โหลด
- ผลการตรวจสอบ SHA-256

ควรทดลองอย่างน้อย 3 รอบต่อกรณี เพื่อเปรียบเทียบผลลัพธ์ได้เหมาะสมยิ่งขึ้น

## 8. Stop Server

หากต้องการหยุด Server ให้กลับไปยัง Terminal ที่เปิด Server แล้วกด

```text
Ctrl + C
```

ตรวจสอบ Port ของ Traditional I/O

```powershell
netstat -ano | findstr :5000
```

ตรวจสอบ Port ของ NIO

```powershell
netstat -ano | findstr :5001
```

หากยังมีโปรเซสอยู่ในสถานะ `LISTENING` ให้ตรวจสอบ PID แล้วปิดด้วยคำสั่ง

```powershell
taskkill /PID <PID> /F
```

เปลี่ยน `<PID>` เป็นหมายเลขโปรเซสที่พบ

## 9. Troubleshooting

### Error: ClassNotFoundException

```text
Error: Could not find or load main class t_Server
Caused by: java.lang.ClassNotFoundException: t_Server
```

ให้ตรวจสอบว่าอยู่ในโฟลเดอร์ที่ถูกต้องและ Compile สำเร็จแล้ว

```powershell
cd "D:\Year_3\OS\project2final"
javac *.java
java t_Server
```

ถ้าเกิดปัญหาเมื่อกด Run Java ใน VS Code ให้เปิดโฟลเดอร์โปรเจกต์ด้วย File → Open Folder และลองใช้ `Java: Clean Java Language Server Workspace`

### Error: Address already in use

```text
java.net.BindException: Address already in use
```

เกิดจาก Port ที่ Server ต้องการใช้งานถูกใช้อยู่แล้ว ให้ตรวจสอบและปิดโปรเซสเดิมก่อนเปิด Server ใหม่

### Error: Connection refused

```text
java.net.ConnectException: Connection refused
```

ให้ตรวจสอบว่า

1. Server เปิดทำงานอยู่
2. Client เชื่อมต่อกับ Port ที่ถูกต้อง
3. Traditional I/O ใช้ Port 5000
4. NIO ใช้ Port 5001

### Error: File not found

ตรวจสอบว่าชื่อไฟล์และ Path ที่กำหนดใน Server ตรงกับไฟล์ที่มีอยู่จริง

## 10. Notes

- ต้องเปิด Server ก่อน Client
- ใช้ Terminal แยกกันระหว่าง Server และ Client
- Traditional I/O และ NIO สามารถใช้ Port ต่างกันได้
- หลังแก้ไขโค้ด `.java` ต้อง Compile ใหม่ก่อนรันด้วยคำสั่ง `java`
- หากต้องการใช้ Run Java ใน VS Code ควรเปิดโฟลเดอร์โปรเจกต์ทั้งหมด ไม่ใช่เปิดเฉพาะไฟล์
- การทดลองด้วย Worker 10 Threads ไม่ได้หมายความว่าความเร็วจะเพิ่มขึ้น 10 เท่าเสมอไป
