# 🧠 OS Scheduler Simulation (Java)

This project is a **Java-based simulation of an Operating System scheduler**, developed as part of an academic milestone.

It demonstrates how an OS handles:

* Process scheduling
* Process Control Blocks (PCB)
* Program execution
* Disk interaction using file-based simulation

---

## 🚀 Features

* Simulates multiple programs running as processes
* Implements a scheduling algorithm (e.g., Round Robin)
* Uses text files to represent memory/disk
* Tracks process states and execution flow

---

## 📁 Project Structure

```
OS-Scheduler/
│
├── src/
│   ├── OS.java              # Main entry point
│   ├── Scheduler.java      # Scheduling logic
│   ├── PCB.java            # Process Control Block
│   └── Program.java        # Program representation
│
├── Program_1.txt           # Input program 1
├── Program_2.txt           # Input program 2
├── Program_3.txt           # Input program 3
├── Disk.txt                # Initial disk state
├── Final output in the disk.png  # Output result
```

---

## ▶️ How to Run

1. Open the project in IntelliJ IDEA or Eclipse
2. Make sure `src/` is marked as **Sources Root**
3. Run:

   ```
   OS.java
   ```
4. Check console output and updated disk file

---

## 🖼️ Final Output

Below is the final state of the disk after execution:

![Final Output](output.png)

---

## 🛠️ Technologies Used

* Java
* File I/O
* Basic OS Scheduling Concepts

---

## 📌 Notes

* Ensure all `.txt` files are in the root directory
* The working directory must be set correctly in your IDE

---

## 👨‍💻 Author

Muhammad Magdy
