# 517-312 Operating Systems

# TERM PROJECT - Mini Job Scheduler & Shared Resource Manager

### Group Member
### 1.นางสาวกัญญรัชต์ สมหวังพรเจริญ รหัสนักศึกษา 670710122
### 2.นางสาวอนัญญณัชช์ ขจรศิริผล รหัสนักศึกษา 670710637
### 3.นางสาวจุฑารัตน์ รู้วงษ์ รหัสนักศึกษา 670710643

## 1. Compile 

เปิด Terminal (PowerShell) ใน VS Code แล้วเข้าโฟลเดอร์โปรเจกต์

```powershell
cd OS101
```
เข้าโฟลเดอร์ src

```powershell
cd src
```
Compile ไฟล์ Java ทั้งหมด
```powershell
javac *.java
```

## 2. Run

รูปแบบคำสั่งสำหรับรันโปรแกรม:

รันดูผลอย่างเดียว (ไม่บันทึก Log)

```text
java -cp . Main "..\OS input\<workload.csv>" <fcfs|priority> <workers> <printerPermits> <databasePermits>
```
สร้าง Raw Logs ไฟล์ใหม่ หรือ รันและบันทึกทับ Log เดิม

```text
java -cp . Main "..\OS input\<workload.csv>" <fcfs|priority> <workers> <printerPermits> <databasePermits> | Tee-Object "..\logs\<log_filename>.txt"
```

**ตัวอย่างการรัน งานที่ 1 - 11**

### 01: Standard FCFS
```powershell
java Main "..\OS input\jobs_standard.csv" fcfs 3 1 2 | Tee-Object ..\logs\01_standard_fcfs.txt
```

### 02: Standard Priority
```powershell
java Main "..\OS input\jobs_standard.csv" priority 3 1 2 | Tee-Object ..\logs\02_standard_priority.txt
```

### 03: Standard Priority & Priority Worker = 1
```powershell
java Main "..\OS input\jobs_standard.csv" priority 1 1 2 | Tee-Object ..\logs\03_priority_worker1.txt
```

### 04: Standard Priority & Priority Worker = 5
```powershell
java Main "..\OS input\jobs_standard.csv" priority 5 1 2 | Tee-Object ..\logs\04_priority_worker5.txt
```

### 05: Printer Priority & Printer Permit = 1
```powershell
java Main "..\OS input\jobs_printer.csv" priority 3 1 2 | Tee-Object ..\logs\05_printer1.txt
```

### 06: Printer Priority & Printer Permit = 2
```powershell
java Main "..\OS input\jobs_printer.csv" priority 3 2 2 | Tee-Object ..\logs\06_printer2.txt
```

### 07: Standard Priority & Priority Repeat
```powershell
java Main "..\OS input\jobs_standard.csv" priority 3 1 2 | Tee-Object ..\logs\07_priority_repeat.txt
```

### 08: Database FCFS & Database Permit = 2
```powershell
java Main "..\OS input\jobs_db.csv" fcfs 3 1 2 | Tee-Object ..\logs\08_database.txt
```

### 09: Same Priority & Priority Scheduling
```powershell
java Main "..\OS input\jobs_same_priority.csv" priority 3 1 2 | Tee-Object ..\logs\09_same_priority.txt
```

### 10: Database FCFS & Database Permit = 1
```powershell
java Main "..\OS input\jobs_db.csv" fcfs 3 1 1 | Tee-Object ..\logs\10_database1.txt
```

### 11: Single FCFS & Priority Worker = 1
```powershell
java Main "..\OS input\jobs_single.csv" fcfs 1 1 2 | Tee-Object ..\logs\11_single_fcfs.txt
```

### ตรวจสอบจำนวนงานที่ประมวลผลเสร็จสิ้น

```powershell
Get-ChildItem "..\logs\*.txt" |
    Sort-Object Name |
    ForEach-Object {
        Write-Host "`n========== $($_.Name) =========="
        Select-String -Path $_.FullName -Pattern "Completed Jobs"
    }
```
### แสดงผลสรุปการรันทั้งหมด

```powershell
Get-ChildItem "..\logs\*.txt" |
    Sort-Object Name |
    ForEach-Object {
        Write-Host "`n========== $($_.Name) =========="
        Get-Content $_.FullName -Tail 8
    }
```



## 3. Command-Line Arguments (ค่าที่ใส่ตอนรันโปรแกรม)

| Argument | Description |
|---|---|
| `workload.csv` | ตำแหน่งไฟล์ CSV ที่เก็บข้อมูล Job |
| `fcfs\|priority` | เลือกการจัดตารางงานแบบ FCFS หรือ Priority |
| `workers` | จำนวน Worker Threads ที่ใช้ประมวลผล Job (มากกว่า 0) |
| `printerPermits` | จำนวน Permits สำหรับ Printer Semaphore (มากกว่า 0) |
| `databasePermits` | จำนวน Permits สำหรับ Database Semaphore (มากกว่า 0) |

**หมายเหตุ:** ตัวอย่างคำสั่งสมมติว่าโฟลเดอร์ `src` และ `OS input` อยู่ภายใต้โฟลเดอร์โปรเจกต์เดียวกัน

## 4. Limitations (ข้อจำกัดที่ควรรู้)

- **Non-preemptive Scheduling:** เมื่อ Job เริ่มทำงานแล้ว จะทำงานต่อจนเสร็จ โดยไม่ถูกขัดจังหวะ แม้ว่าจะมี Job ใหม่ที่มี Priority สูงกว่าเข้ามาก็ตาม
- **Shared Resources:** Job แต่ละตัวสามารถใช้ทรัพยากรได้เพียง 1 ประเภท คือ `NONE`, `PRINTER` หรือ `DATABASE`
- **Semaphore Permits:** จำนวน Job ที่สามารถใช้ Printer หรือ Database พร้อมกันได้ จะขึ้นอยู่กับจำนวน Permit ที่กำหนดไว้
- **Execution Time:** ค่า Waiting Time, Turnaround Time และ Throughput อาจไม่เท่ากันในแต่ละรอบที่ทดลอง เนื่องจากมีหลาย Thread ทำงานพร้อมกัน และระบบปฏิบัติการอาจจัดสรร CPU แตกต่างกันในแต่ละครั้ง
- **CSV Format:** ไฟล์ Workload ต้องมีข้อมูลตามรูปแบบที่โปรแกรมกำหนด และต้องระบุ Path ให้ถูกต้อง เพื่อให้โปรแกรมสามารถอ่านไฟล์ได้
- **Monitor Snapshot:** ข้อมูลจำนวน Permit ของ Printer และ Database กับสถานะ READY, RUNNING และ COMPLETED อาจถูกอ่านในช่วงเวลาที่ต่างกันเล็กน้อย ทำให้ค่าที่แสดงอาจไม่ใช่สถานะของระบบในเวลาเดียวกันทั้งหมด