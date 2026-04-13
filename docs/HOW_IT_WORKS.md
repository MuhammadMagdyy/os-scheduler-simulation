# How It Works (OS-Scheduler)

This project contains **two simulation engines**:

- **Legacy** (original assignment): `src/Scheduler.java` + `src/OS.java`
- **Realistic** (newer, OS-like): `src/RealisticScheduler.java` + `src/RealisticOS.java`

The Swing app `src/SchedulerUI.java` lets you run either mode and view outputs.

---

## 1) Program Files & Instruction Model

### Program file format

- Each program is a plain text file (for example: `Program_1.txt`).
- **Each line is one instruction**.
- The UI lets you browse/select program files and edit **arrival times**.

### Supported instructions (Realistic mode)

Realistic mode parses each line using whitespace splitting, so tokens are separated by spaces.

#### Semaphores (mutexes)

- `semWait <name>`
  - If `<name>` is free: current process becomes the owner.
  - If owned by another process: current process becomes `BLOCKED` and is placed in the mutex blocked FIFO queue.
- `semSignal <name>`
  - If current process is not the owner: it is ignored (logged as “not owner”).
  - If no blocked processes: mutex becomes free.
  - If there is a blocked process: ownership transfers to the next blocked process and that process becomes `READY`.

Built-in mutex names created at startup:

- `userInput`, `userOutput`, `file`

#### Variables

Each process has a private map of variables (`String -> String`) stored in `src/SimProcess.java`.

- `assign <var> input`
  - Requests a value from the input provider (CLI prompt or UI Input tab).
- `assign <var> readFile <fileVar>`
  - Reads the whole file named by variable `<fileVar>` and stores it into `<var>`.
- `assign <var> <literal...>`
  - Stores the rest of the line as a literal string (joined with spaces).

#### Output

- `print <var>`
  - Prints the value of `<var>` (or `null` if unset).
- `printFromTo <fromVar> <toVar>`
  - Treats both variables as integers and prints the range.

#### File I/O

- `writeFile <fileVar> <contentVar>`
  - Writes the value of `<contentVar>` into the file named by `<fileVar>`.

Notes:

- Realistic mode treats file reads/writes as **instant** operations (no extra I/O delay yet).
- Legacy mode has its own file model (`Disk.txt` / `FinalDisk.txt`) used for swapping.

---

## 2) Scheduling & Time Model (Realistic)

### Scheduling policy

Realistic mode supports multiple algorithms:

- **ROUND_ROBIN (RR)**: time-sliced ready queue (uses Quantum)
- **FCFS**: First Come First Serve (non-preemptive)
- **SJF**: Shortest Job First (non-preemptive, based on remaining instruction count at dispatch)
- **PRIORITY**: Non-preemptive priority scheduling (smaller number = higher priority)

#### Round Robin (RR)

- Processes enter `READY` at their `arrivalTime`.
- CPU takes the head of the ready queue and runs it for at most **Quantum** instruction slots.
- If a process blocks during its slice, it stops immediately and the CPU picks the next ready process.

#### FCFS / SJF / PRIORITY

- CPU selects a process based on the algorithm, then runs it **until it blocks or finishes**.
- If a process blocks, it leaves the CPU immediately and the next process is selected.
- SJF uses `remainingInstructions = totalInstructions - pc` when selecting the next process.
- PRIORITY uses the `priority` column in the UI’s Programs table (smaller value = higher priority).

### Time rules

- **Time increases by 1 per instruction slot**.
- If the CPU has no ready processes:
  - Time still advances by 1 and an `CPU_IDLE` event is recorded.
- Arrivals are admitted when `arrivalTime <= currentTime`.

### Process states

Realistic mode uses `src/ProcessState.java`:

- `NEW` → `READY` → `RUNNING` → (`BLOCKED` → `READY`)* → `FINISHED`

Timing metrics stored in `src/SimProcess.java`:

- `firstRunTime`: first time the process was dispatched
- `finishTime`: time when the process became `FINISHED`
- Response time = `firstRunTime - arrivalTime`
- Turnaround time = `finishTime - arrivalTime`

---

## 3) Trace Events (Realistic)

Realistic mode emits two forms of trace:

1) Human-friendly log lines (console)
2) Structured events (`src/SimulationEvent.java`)

### Event types

See `SimulationEvent.Type` for the full list. Common ones:

- `ARRIVE`, `CPU_DISPATCH`, `INSTRUCTION`, `BLOCK`, `UNBLOCK`, `FINISH`, `CPU_IDLE`
- `MUTEX_ACQUIRE`, `MUTEX_RELEASE`, `MUTEX_TRANSFER`
- `INPUT_REQUEST`, `INPUT_VALUE`
- `OUTPUT`, `FILE_READ`, `FILE_WRITE`

### Export format (CSV)

From the UI: **File → Export Trace (CSV)**.

CSV columns:

- `time`: integer time unit
- `pid`: process id (blank if not tied to a PID)
- `type`: event type
- `message`: a short description (escaped for CSV)

This CSV is designed to be easy to load in Excel/Python for plotting.

---

## 4) UI Guide (SchedulerUI)

Main UI file: `src/SchedulerUI.java`

### Controls

- **Quantum**: RR slice length
- **Mode**
  - `Legacy`: runs `src/Scheduler.java` and shows memory/disk snapshots
  - `Realistic`: runs `src/RealisticScheduler.java` and shows Summary/Timeline
- **Algorithm** (Realistic only)
  - `ROUND_ROBIN`, `FCFS`, `SJF`, `PRIORITY`
  - Quantum is used only for `ROUND_ROBIN`
- **Run**: executes the simulation in a background thread

### Tabs

- **Console**: everything printed to stdout/stderr is redirected here
- **Memory** (Legacy): snapshot of the legacy `Object[] memory`
- **FinalDisk** (Legacy): final content written by the legacy swap buffer
- **Summary** (Realistic): per-process metrics + final variable map
- **Timeline** (Realistic): structured events table + a simple CPU-per-tick Gantt text view
- **Input** (Realistic): when a process executes `assign X input`, the scheduler pauses until you submit a value here
- **Docs**: built-in documentation (HTML)

---

## 5) Legacy Mode (What It Is)

Legacy scheduler: `src/Scheduler.java`

- Uses a fixed memory array (`Object[40]`).
- Builds PCB-like metadata in memory and in a PCB queue (`src/PCB.java`).
- Simulates swapping by writing a memory segment to `Disk.txt` and later loading it back.
- Writes a final snapshot to `FinalDisk.txt`.

Legacy mode is intentionally left mostly unchanged to preserve the original assignment behavior.

---

## 6) How To Extend It

### Add a new instruction (Realistic)

1. Edit `src/RealisticScheduler.java`
2. Add a new `case` in `executeInstruction(...)`
3. Emit a structured `SimulationEvent` (so it shows in Timeline/CSV)
4. Update docs (this file + the in-app Docs HTML in `src/SchedulerUI.java`)

### Add a new metric

1. Store fields on `src/SimProcess.java`
2. Update `RealisticSummaryTableModel` in `src/SchedulerUI.java`

### Add an I/O delay model (recommended next)

Right now `readFile/writeFile` are instant. A more realistic model would:

- Introduce a device queue for “file I/O”
- Block the process for N ticks while I/O completes
- Emit `IO_START` / `IO_COMPLETE` events

---

## 7) Quick Entry Points

- UI: `src/SchedulerUI.java`
- Legacy CLI: `src/OS.java`
- Realistic CLI: `src/RealisticOS.java`
