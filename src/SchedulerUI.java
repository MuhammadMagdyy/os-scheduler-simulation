import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JEditorPane;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JSpinner;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import javax.swing.table.AbstractTableModel;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.Queue;
import java.util.Comparator;
import java.util.concurrent.SynchronousQueue;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;

/**
 * Swing UI for running the scheduler simulation.
 *
 * <p>Modes:
 * <ul>
 *   <li><b>Legacy</b>: runs the original {@link Scheduler} and shows memory + disk snapshots.</li>
 *   <li><b>Realistic</b>: runs {@link RealisticScheduler} with process blocking/unblocking and an in-app input panel.</li>
 * </ul>
 *
 * <p>All console output is redirected into the "Console" tab.</p>
 */
public class SchedulerUI extends JFrame {

	private final JTextArea console = new JTextArea();
	private final MemoryTableModel memoryModel = new MemoryTableModel();
	private final JTable memoryTable = new JTable(memoryModel);
	private final JTextArea finalDiskArea = new JTextArea();
	private final JEditorPane docsPane = new JEditorPane("text/html", buildDocsHtml());
	private final JTextArea fullDocsArea = new JTextArea();
	private final RealisticSummaryTableModel realisticSummaryModel = new RealisticSummaryTableModel();
	private final JTable realisticSummaryTable = new JTable(realisticSummaryModel);
	private final EventsTableModel eventsModel = new EventsTableModel();
	private final JTable eventsTable = new JTable(eventsModel);
	private final JTextArea ganttArea = new JTextArea();

	private final JLabel inputRequestLabel = new JLabel("No pending input.");
	private final JTextField inputField = new JTextField(24);
	private final JButton inputSubmitButton = new JButton("Submit");
	private volatile SynchronousQueue<String> pendingInput = null;

	private final JButton runButton = new JButton("Run");
	private final JButton clearButton = new JButton("Clear");
	private final JButton browseButton = new JButton("Browse...");
	private final JComboBox<String> modeCombo = new JComboBox<>(new String[] {"Legacy", "Realistic"});
	private final JComboBox<SchedulingAlgorithm> algorithmCombo = new JComboBox<>(SchedulingAlgorithm.values());
	private final JSpinner quantumSpinner = new JSpinner(new SpinnerNumberModel(2, 1, 100, 1));
	private final ProgramsTableModel programsModel = new ProgramsTableModel();
	private final JTable programsTable = new JTable(programsModel);

	private final JTabbedPane tabs = new JTabbedPane();
	private final JLabel statusLabel = new JLabel("Ready.");

	public SchedulerUI() {
		super("OS Scheduler Simulator");
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		setMinimumSize(new Dimension(920, 640));

		console.setEditable(false);
		console.setLineWrap(true);
		console.setWrapStyleWord(true);
		console.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));

		finalDiskArea.setEditable(false);
		finalDiskArea.setLineWrap(true);
		finalDiskArea.setWrapStyleWord(true);
		finalDiskArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));

		ganttArea.setEditable(false);
		ganttArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));

		docsPane.setEditable(false);
		docsPane.putClientProperty("JEditorPane.honorDisplayProperties", Boolean.TRUE);
		docsPane.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 13));

		fullDocsArea.setEditable(false);
		fullDocsArea.setLineWrap(true);
		fullDocsArea.setWrapStyleWord(true);
		fullDocsArea.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 13));
		reloadFullDocs();

		JPanel header = buildHeaderPanel();
		JPanel controls = buildControlsPanel();
		JPanel top = new JPanel(new BorderLayout());
		top.add(header, BorderLayout.NORTH);
		top.add(controls, BorderLayout.SOUTH);

		JScrollPane tableScroll = new JScrollPane(programsTable);
		tableScroll.setBorder(BorderFactory.createTitledBorder("Programs"));
		tableScroll.setPreferredSize(new Dimension(920, 170));
		programsTable.setRowHeight(24);

		JScrollPane consoleScroll = new JScrollPane(console);
		tabs.addTab("Console", consoleScroll);

		JScrollPane memoryScroll = new JScrollPane(memoryTable);
		tabs.addTab("Memory", memoryScroll);
		memoryTable.setRowHeight(22);

		JScrollPane diskScroll = new JScrollPane(finalDiskArea);
		tabs.addTab("FinalDisk", diskScroll);

		JScrollPane summaryScroll = new JScrollPane(realisticSummaryTable);
		tabs.addTab("Summary", summaryScroll);
		realisticSummaryTable.setRowHeight(22);

		JPanel timelinePanel = new JPanel(new BorderLayout(10, 10));
		timelinePanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
		eventsTable.setRowHeight(22);
		JScrollPane eventsScroll = new JScrollPane(eventsTable);
		eventsScroll.setBorder(BorderFactory.createTitledBorder("Timeline Events (Realistic mode)"));
		JScrollPane ganttScroll = new JScrollPane(ganttArea);
		ganttScroll.setBorder(BorderFactory.createTitledBorder("Gantt (CPU per time unit)"));
		ganttScroll.setPreferredSize(new Dimension(920, 160));
		timelinePanel.add(eventsScroll, BorderLayout.CENTER);
		timelinePanel.add(ganttScroll, BorderLayout.SOUTH);
		tabs.addTab("Timeline", timelinePanel);

		tabs.addTab("Input", buildInputPanel());
		tabs.addTab("Docs", buildDocsPanel());

		JPanel center = new JPanel(new BorderLayout(10, 10));
		center.add(tableScroll, BorderLayout.NORTH);
		center.add(tabs, BorderLayout.CENTER);

		setLayout(new BorderLayout(10, 10));
		setJMenuBar(buildMenuBar());
		add(top, BorderLayout.NORTH);
		add(center, BorderLayout.CENTER);
		add(buildStatusBar(), BorderLayout.SOUTH);

		wireActions();
		updateControlState();
		redirectStdoutToConsole();
		pack();
		setLocationRelativeTo(null);
	}

	private JPanel buildHeaderPanel() {
		JLabel title = new JLabel("OS Scheduler Simulator");
		title.setFont(title.getFont().deriveFont(Font.BOLD, 18f));

		JLabel subtitle = new JLabel("Legacy + Realistic modes • Round Robin • Semaphores • Memory/Disk snapshots");
		subtitle.setForeground(new Color(90, 90, 90));

		JPanel panel = new JPanel(new GridBagLayout());
		panel.setBorder(BorderFactory.createEmptyBorder(10, 12, 0, 12));

		GridBagConstraints c = new GridBagConstraints();
		c.insets = new Insets(0, 0, 2, 0);
		c.gridx = 0;
		c.gridy = 0;
		c.anchor = GridBagConstraints.WEST;
		panel.add(title, c);

		c.gridy = 1;
		c.insets = new Insets(0, 0, 0, 0);
		panel.add(subtitle, c);

		return panel;
	}

	/**
	 * Creates a small menu bar with a built-in "How it works" help dialog.
	 */
	private JMenuBar buildMenuBar() {
		JMenuBar bar = new JMenuBar();

		JMenu file = new JMenu("File");
		JMenuItem export = new JMenuItem("Export Trace (CSV)...");
		export.addActionListener(e -> exportTraceCsv());
		file.add(export);
		bar.add(file);

		JMenu help = new JMenu("Help");
		JMenuItem openDocs = new JMenuItem("Open Docs");
		openDocs.addActionListener(e -> selectTab("Docs"));
		help.add(openDocs);

		JMenuItem reloadDocs = new JMenuItem("Reload Full Docs");
		reloadDocs.addActionListener(e -> reloadFullDocs());
		help.add(reloadDocs);

		JMenuItem how = new JMenuItem("How It Works (Popup)");
		how.addActionListener(e -> showHowItWorksPopup());
		help.add(how);

		JMenuItem about = new JMenuItem("About");
		about.addActionListener(e -> JOptionPane.showMessageDialog(this,
				"OS Scheduler Simulation (Java)\nModes: Legacy + Realistic\nAuthor: Muhammad Magdy",
				"About",
				JOptionPane.INFORMATION_MESSAGE));
		help.add(about);

		bar.add(help);
		return bar;
	}

	private JPanel buildStatusBar() {
		JPanel panel = new JPanel(new BorderLayout());
		panel.setBorder(BorderFactory.createEmptyBorder(6, 10, 6, 10));
		panel.add(statusLabel, BorderLayout.WEST);
		return panel;
	}

	private void setStatus(String status) {
		SwingUtilities.invokeLater(() -> statusLabel.setText(status));
	}

	private void showHowItWorksPopup() {
		JOptionPane.showMessageDialog(this, docsPane, "How It Works", JOptionPane.INFORMATION_MESSAGE);
	}

	private JPanel buildControlsPanel() {
		JPanel panel = new JPanel(new GridBagLayout());
		panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 0, 10));

		GridBagConstraints c = new GridBagConstraints();
		c.insets = new Insets(4, 4, 4, 4);
		c.gridy = 0;
		c.gridx = 0;
		c.anchor = GridBagConstraints.WEST;
		panel.add(new JLabel("Quantum:"), c);

		c.gridx = 1;
		panel.add(quantumSpinner, c);

		c.gridx = 2;
		panel.add(new JLabel("Mode:"), c);

		c.gridx = 3;
		panel.add(modeCombo, c);

		c.gridx = 4;
		panel.add(new JLabel("Algorithm:"), c);

		c.gridx = 5;
		panel.add(algorithmCombo, c);

		c.gridx = 6;
		c.weightx = 1;
		panel.add(new JPanel(), c);

		JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
		buttons.add(browseButton);
		buttons.add(clearButton);
		buttons.add(runButton);

		c.gridx = 7;
		c.weightx = 0;
		c.anchor = GridBagConstraints.EAST;
		panel.add(buttons, c);

		return panel;
	}

	private void wireActions() {
		clearButton.addActionListener(e -> console.setText(""));
		modeCombo.addActionListener(e -> updateControlState());
		algorithmCombo.addActionListener(e -> updateControlState());

		browseButton.addActionListener(e -> {
			int row = programsTable.getSelectedRow();
			if (row < 0) {
				JOptionPane.showMessageDialog(this, "Select a program row first.", "No selection", JOptionPane.WARNING_MESSAGE);
				return;
			}

			JFileChooser chooser = new JFileChooser(new File("."));
			chooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
			int result = chooser.showOpenDialog(this);
			if (result == JFileChooser.APPROVE_OPTION) {
				File selected = chooser.getSelectedFile();
				programsModel.setProgramPath(row, selected.getPath());
			}
		});

		runButton.addActionListener(e -> runSimulation());
		inputSubmitButton.addActionListener(e -> submitInput());
		inputField.addActionListener(e -> submitInput());
	}

	private void runSimulation() {
		int quantum = ((Number) quantumSpinner.getValue()).intValue();
		ProgramsTableModel.ProgramRow[] rows = programsModel.snapshot();
		String mode = String.valueOf(modeCombo.getSelectedItem());
		SchedulingAlgorithm algorithm = (SchedulingAlgorithm) algorithmCombo.getSelectedItem();

		for (ProgramsTableModel.ProgramRow row : rows) {
			if (row.path == null || row.path.isBlank()) {
				JOptionPane.showMessageDialog(this, "Program file path is missing for PID " + row.pid + ".", "Invalid input", JOptionPane.ERROR_MESSAGE);
				return;
			}
			if (!new File(row.path).exists()) {
				JOptionPane.showMessageDialog(this, "Program file not found: " + row.path, "Invalid input", JOptionPane.ERROR_MESSAGE);
				return;
			}
			if (row.arrivalTime < 0) {
				JOptionPane.showMessageDialog(this, "Arrival time must be >= 0 for PID " + row.pid + ".", "Invalid input", JOptionPane.ERROR_MESSAGE);
				return;
			}
		}

		runButton.setEnabled(false);
		browseButton.setEnabled(false);
		memoryModel.setMemory(new Object[0]);
		finalDiskArea.setText("");
		realisticSummaryModel.setProcesses(List.of(), 0);
		eventsModel.setEvents(List.of());
		ganttArea.setText("");
		setInputEnabled(false);
		setStatus("Running (" + mode + "), quantum=" + quantum + "...");

		new SwingWorker<Void, Void>() {
			private Object[] finalMemory = new Object[0];
			private List<Object> finalDisk = List.of();
			private List<SimProcess> realisticProcesses = List.of();
			private int realisticTime = 0;
			private List<SimulationEvent> realisticEvents = List.of();

			@Override
			protected Void doInBackground() throws Exception {
				Queue<Program> programs = new LinkedList<>();
				List<SimProcess> processes = new ArrayList<>();
				for (ProgramsTableModel.ProgramRow row : rows) {
					Program p = new Program(row.pid, row.path, row.arrivalTime, 0);
					programs.add(p);
					processes.add(new SimProcess(row.pid, row.arrivalTime, new ArrayList<>(p.instruction), row.priority));
				}

				if ("Realistic".equals(mode)) {
					RealisticScheduler realisticScheduler = new RealisticScheduler(processes, quantum, algorithm, SchedulerUI.this::promptInputBlocking);
					realisticScheduler.run();
					realisticProcesses = realisticScheduler.getAllProcesses();
					realisticTime = realisticScheduler.getTime();
					realisticEvents = realisticScheduler.getEvents();
				} else {
					Scheduler scheduler = new Scheduler(programs, quantum);
					finalMemory = scheduler.getMemorySnapshot();
					finalDisk = scheduler.getFinalDiskSnapshot();
				}
				return null;
			}

			@Override
			protected void done() {
				try {
					get();
					if (!"Realistic".equals(mode)) {
						memoryModel.setMemory(finalMemory);
						finalDiskArea.setText(formatLines(finalDisk));
						selectTab("Memory");
						setStatus("Done (Legacy).");
					} else {
						realisticSummaryModel.setProcesses(realisticProcesses, realisticTime);
						eventsModel.setEvents(realisticEvents);
						ganttArea.setText(buildGanttText(realisticEvents));
						selectTab("Timeline");
						setStatus("Done (Realistic).");
					}
				} catch (Exception ex) {
					System.err.println("Simulation failed: " + ex.getMessage());
					ex.printStackTrace(System.err);
					setStatus("Failed: " + ex.getMessage());
				} finally {
					runButton.setEnabled(true);
					browseButton.setEnabled(true);
					setInputEnabled(false);
				}
			}
		}.execute();
	}

	private void updateControlState() {
		String mode = String.valueOf(modeCombo.getSelectedItem());
		boolean realistic = "Realistic".equals(mode);
		algorithmCombo.setEnabled(realistic);
		SchedulingAlgorithm algorithm = (SchedulingAlgorithm) algorithmCombo.getSelectedItem();
		boolean rr = algorithm == SchedulingAlgorithm.ROUND_ROBIN;
		quantumSpinner.setEnabled(!realistic || rr);
	}

	/**
	 * Input provider for realistic mode.
	 *
	 * <p>This blocks the simulation thread until the user submits a value in the Input tab.</p>
	 */
	private String promptInputBlocking(int pid, String variableName) {
		SynchronousQueue<String> queue = new SynchronousQueue<>();
		pendingInput = queue;

		SwingUtilities.invokeLater(() -> {
			setInputEnabled(true);
			inputRequestLabel.setText("P" + pid + " enter value for " + variableName + ":");
			inputField.setText("");
			inputField.requestFocusInWindow();
			selectTab("Input");
			setStatus("Waiting for input (P" + pid + ", " + variableName + ")...");
		});

		try {
			String value = queue.take();
			SwingUtilities.invokeLater(() -> {
				inputRequestLabel.setText("No pending input.");
				setInputEnabled(false);
			});
			return value;
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			throw new RuntimeException("Input interrupted", e);
		} finally {
			pendingInput = null;
		}
	}

	private void submitInput() {
		SynchronousQueue<String> queue = pendingInput;
		if (queue == null) {
			return;
		}
		String value = inputField.getText();
		new Thread(() -> {
			try {
				queue.put(value);
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
			}
		}, "input-submit").start();
	}

	private JPanel buildInputPanel() {
		JPanel panel = new JPanel(new GridBagLayout());
		panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

		GridBagConstraints c = new GridBagConstraints();
		c.insets = new Insets(6, 6, 6, 6);
		c.gridx = 0;
		c.gridy = 0;
		c.anchor = GridBagConstraints.WEST;
		panel.add(new JLabel("Realistic mode will pause and request values for `assign X input`."), c);

		c.gridy = 1;
		panel.add(inputRequestLabel, c);

		c.gridy = 2;
		c.fill = GridBagConstraints.HORIZONTAL;
		c.weightx = 1;
		panel.add(inputField, c);

		c.gridx = 1;
		c.weightx = 0;
		c.fill = GridBagConstraints.NONE;
		panel.add(inputSubmitButton, c);

		setInputEnabled(false);
		return panel;
	}

	private JPanel buildDocsPanel() {
		JTabbedPane docsTabs = new JTabbedPane();
		docsTabs.addTab("Quick", new JScrollPane(docsPane));
		docsTabs.addTab("Full (HOW_IT_WORKS.md)", buildFullDocsPanel());

		JPanel panel = new JPanel(new BorderLayout());
		panel.add(docsTabs, BorderLayout.CENTER);
		return panel;
	}

	private JPanel buildFullDocsPanel() {
		JPanel top = new JPanel(new BorderLayout());
		top.setBorder(BorderFactory.createEmptyBorder(6, 6, 6, 6));
		top.add(new JLabel("Source: docs/HOW_IT_WORKS.md"), BorderLayout.WEST);
		JButton reload = new JButton("Reload");
		reload.addActionListener(e -> reloadFullDocs());
		top.add(reload, BorderLayout.EAST);

		JPanel panel = new JPanel(new BorderLayout());
		panel.add(top, BorderLayout.NORTH);
		panel.add(new JScrollPane(fullDocsArea), BorderLayout.CENTER);
		return panel;
	}

	private void selectTab(String title) {
		for (int i = 0; i < tabs.getTabCount(); i++) {
			if (title.equals(tabs.getTitleAt(i))) {
				tabs.setSelectedIndex(i);
				return;
			}
		}
	}

	private void reloadFullDocs() {
		fullDocsArea.setText(loadFullDocsText());
		fullDocsArea.setCaretPosition(0);
	}

	private static String loadFullDocsText() {
		try {
			Path path = Path.of("docs", "HOW_IT_WORKS.md");
			if (!Files.exists(path)) {
				return "docs/HOW_IT_WORKS.md not found.\n\nEnsure you are running from the project root and the docs/ folder exists.";
			}
			return Files.readString(path, StandardCharsets.UTF_8);
		} catch (IOException e) {
			return "Failed to load docs/HOW_IT_WORKS.md: " + e.getMessage();
		}
	}

	private void setInputEnabled(boolean enabled) {
		inputField.setEnabled(enabled);
		inputSubmitButton.setEnabled(enabled);
	}

	private static String formatLines(List<Object> lines) {
		StringBuilder sb = new StringBuilder();
		for (Object line : lines) {
			sb.append(String.valueOf(line)).append('\n');
		}
		return sb.toString();
	}

	private static String buildGanttText(List<SimulationEvent> events) {
		List<SimulationEvent> ordered = new ArrayList<>(events);
		ordered.sort(Comparator.comparingInt(e -> e.time));

		int maxTime = -1;
		for (SimulationEvent e : ordered) maxTime = Math.max(maxTime, e.time);
		if (maxTime < 0) return "";

		String[] cpu = new String[maxTime + 1];
		for (int t = 0; t <= maxTime; t++) cpu[t] = "IDLE";

		for (SimulationEvent e : ordered) {
			if (e.type == SimulationEvent.Type.CPU_DISPATCH && e.pid != null) {
				cpu[e.time] = "P" + e.pid;
			} else if (e.type == SimulationEvent.Type.CPU_IDLE) {
				cpu[e.time] = "IDLE";
			}
		}

		StringBuilder sb = new StringBuilder();
		sb.append("time : cpu\n");
		for (int t = 0; t <= maxTime; t++) {
			sb.append(String.format("%4d : %s%n", t, cpu[t]));
		}
		return sb.toString();
	}

	private void exportTraceCsv() {
		if (eventsModel.getRowCount() == 0) {
			JOptionPane.showMessageDialog(this, "No trace events to export. Run Realistic mode first.", "Nothing to export", JOptionPane.INFORMATION_MESSAGE);
			return;
		}
		JFileChooser chooser = new JFileChooser(new File("."));
		chooser.setSelectedFile(new File("trace.csv"));
		int result = chooser.showSaveDialog(this);
		if (result != JFileChooser.APPROVE_OPTION) return;
		File target = chooser.getSelectedFile();
		try {
			Files.writeString(target.toPath(), eventsModel.toCsv(), StandardCharsets.UTF_8);
			JOptionPane.showMessageDialog(this, "Exported: " + target.getPath(), "Export complete", JOptionPane.INFORMATION_MESSAGE);
		} catch (IOException e) {
			JOptionPane.showMessageDialog(this, "Failed to export: " + e.getMessage(), "Export error", JOptionPane.ERROR_MESSAGE);
		}
	}

	private void redirectStdoutToConsole() {
		PrintStream printStream = new PrintStream(new TextAreaOutputStream(console), true, StandardCharsets.UTF_8);
		System.setOut(printStream);
		System.setErr(printStream);
	}

	public static void main(String[] args) {
		installLookAndFeel();
		SwingUtilities.invokeLater(() -> new SchedulerUI().setVisible(true));
	}

	/**
	 * Installs a more modern-looking Swing theme using the built-in Nimbus Look & Feel.
	 *
	 * <p>This requires no external libraries, so it works in typical university/IDE setups.</p>
	 */
	private static void installLookAndFeel() {
		try {
			UIManager.setLookAndFeel("javax.swing.plaf.nimbus.NimbusLookAndFeel");
			UIManager.put("control", new Color(245, 246, 248));
			UIManager.put("info", new Color(242, 242, 242));
			UIManager.put("nimbusBase", new Color(60, 80, 160));
			UIManager.put("nimbusBlueGrey", new Color(180, 190, 205));
			UIManager.put("nimbusLightBackground", Color.WHITE);
			UIManager.put("text", Color.DARK_GRAY);
		} catch (ClassNotFoundException | InstantiationException | IllegalAccessException | UnsupportedLookAndFeelException ignored) {
			// Fall back to the platform look & feel if Nimbus isn't available.
		}
	}

	private static String buildDocsHtml() {
		return """
				<html>
				  <head>
				    <style>
				      body { font-family: sans-serif; font-size: 13px; padding: 10px; }
				      h1 { font-size: 18px; margin: 0 0 8px 0; }
				      h2 { font-size: 14px; margin: 14px 0 6px 0; }
				      code { background: #f2f3f5; padding: 1px 4px; border-radius: 4px; }
				      .pill { display:inline-block; background:#eef2ff; color:#243b87; padding:2px 8px; border-radius:999px; font-size:12px; }
				      ul { margin-top: 6px; }
				    </style>
				  </head>
				  <body>
				    <h1>How This Simulator Works</h1>
				    <div class="pill">Round Robin</div> <div class="pill">Semaphores</div> <div class="pill">Disk/Memory</div>

				    <h2>Quick Start</h2>
				    <ul>
				      <li>Set <b>Quantum</b> (instructions per CPU turn).</li>
				      <li>Edit the <b>Programs</b> table (file path + arrival time).</li>
				      <li>Choose <b>Mode</b>, choose <b>Algorithm</b> (Realistic), and click <b>Run</b>.</li>
				    </ul>

				    <h2>Program Files</h2>
				    <ul>
				      <li>Each program is a text file.</li>
				      <li>Each line is treated as one instruction.</li>
				    </ul>

				    <h2>Legacy Mode</h2>
				    <ul>
				      <li>Runs the original <code>Scheduler</code> implementation (kept as-is).</li>
				      <li><b>Memory</b> tab shows a snapshot of the legacy memory array after execution.</li>
				      <li><b>FinalDisk</b> tab shows the final disk buffer written by the legacy swap logic.</li>
				    </ul>

				    <h2>Realistic Mode</h2>
				    <ul>
				      <li>Runs <code>RealisticScheduler</code> with process states:
				        <code>NEW</code> → <code>READY</code> → <code>RUNNING</code> → <code>BLOCKED</code> → <code>READY</code> → <code>FINISHED</code>.
				      </li>
				      <li><code>semWait X</code>: acquire mutex <code>X</code> or block in FIFO queue.</li>
				      <li><code>semSignal X</code>: release mutex or transfer ownership to the next blocked process.</li>
				      <li><code>assign a input</code>: requests a value and waits for you in the <b>Input</b> tab.</li>
				      <li><b>Summary</b> tab shows per-process timing (start/finish/turnaround/response) and final variables.</li>
				      <li><b>Timeline</b> tab shows structured events, and <b>File → Export Trace</b> saves them as CSV.</li>
				      <li><b>Algorithms</b>: <code>ROUND_ROBIN</code> (uses Quantum), <code>FCFS</code>, <code>SJF</code>, <code>PRIORITY</code> (lower number = higher priority).</li>
				      <li>The <b>Algorithm</b> selector is enabled only when <b>Mode = Realistic</b>.</li>
				      <li>Quantum is used only for <code>ROUND_ROBIN</code> and is ignored for <code>FCFS/SJF/PRIORITY</code>.</li>
				      <li>The <b>Priority</b> column in the Programs table is used by <code>PRIORITY</code>.</li>
				    </ul>

				    <h2>Where To Look In Code</h2>
				    <ul>
				      <li>UI: <code>SchedulerUI.java</code></li>
				      <li>Legacy scheduler: <code>Scheduler.java</code></li>
				      <li>Realistic engine: <code>RealisticScheduler.java</code>, <code>Mutex.java</code>, <code>SimProcess.java</code></li>
				      <li>Trace events: <code>SimulationEvent.java</code></li>
				    </ul>
				  </body>
				</html>
				""";
	}

	/**
	 * OutputStream that appends text into a Swing {@link JTextArea}.
	 *
	 * <p>Used to redirect stdout/stderr so legacy schedulers that use System.out can
	 * still be displayed inside the UI.</p>
	 */
	private static final class TextAreaOutputStream extends OutputStream {
		private final JTextArea textArea;
		private final StringBuilder buffer = new StringBuilder();

		private TextAreaOutputStream(JTextArea textArea) {
			this.textArea = textArea;
		}

		@Override
		public synchronized void write(int b) throws IOException {
			char c = (char) b;
			buffer.append(c);
			if (c == '\n') {
				flush();
			}
		}

		@Override
		public synchronized void flush() {
			if (buffer.isEmpty()) return;
			String text = buffer.toString();
			buffer.setLength(0);

			SwingUtilities.invokeLater(() -> {
				textArea.append(text);
				textArea.setCaretPosition(textArea.getDocument().getLength());
			});
		}
	}

	/**
	 * Editable table model for selecting program files + arrival times.
	 */
	private static final class ProgramsTableModel extends AbstractTableModel {
		private static final String[] COLUMNS = {"PID", "Program File", "Arrival Time", "Priority"};

		private final ProgramRow[] rows = new ProgramRow[] {
				new ProgramRow(1, "Program_1.txt", 0, 0),
				new ProgramRow(2, "Program_2.txt", 1, 0),
				new ProgramRow(3, "Program_3.txt", 4, 0),
		};

		@Override
		public int getRowCount() {
			return rows.length;
		}

		@Override
		public int getColumnCount() {
			return COLUMNS.length;
		}

		@Override
		public String getColumnName(int column) {
			return COLUMNS[column];
		}

		@Override
		public Class<?> getColumnClass(int columnIndex) {
			return switch (columnIndex) {
				case 0 -> Integer.class;
				case 1 -> String.class;
				case 2 -> Integer.class;
				case 3 -> Integer.class;
				default -> Object.class;
			};
		}

		@Override
		public boolean isCellEditable(int rowIndex, int columnIndex) {
			return columnIndex != 0;
		}

		@Override
		public Object getValueAt(int rowIndex, int columnIndex) {
			ProgramRow row = rows[rowIndex];
			return switch (columnIndex) {
				case 0 -> row.pid;
				case 1 -> row.path;
				case 2 -> row.arrivalTime;
				case 3 -> row.priority;
				default -> null;
			};
		}

		@Override
		public void setValueAt(Object aValue, int rowIndex, int columnIndex) {
			ProgramRow row = rows[rowIndex];
			if (columnIndex == 1) {
				row.path = aValue == null ? "" : aValue.toString();
			} else if (columnIndex == 2) {
				if (aValue instanceof Number n) row.arrivalTime = n.intValue();
				else row.arrivalTime = Integer.parseInt(aValue.toString().trim());
			} else if (columnIndex == 3) {
				if (aValue instanceof Number n) row.priority = n.intValue();
				else row.priority = Integer.parseInt(aValue.toString().trim());
			}
			fireTableCellUpdated(rowIndex, columnIndex);
		}

		void setProgramPath(int rowIndex, String path) {
			rows[rowIndex].path = path;
			fireTableCellUpdated(rowIndex, 1);
		}

		ProgramRow[] snapshot() {
			ProgramRow[] copy = new ProgramRow[rows.length];
			for (int i = 0; i < rows.length; i++) {
				ProgramRow r = rows[i];
				copy[i] = new ProgramRow(r.pid, r.path, r.arrivalTime, r.priority);
			}
			return copy;
		}

		private static final class ProgramRow {
			private final int pid;
			private String path;
			private int arrivalTime;
			private int priority;

			private ProgramRow(int pid, String path, int arrivalTime, int priority) {
				this.pid = pid;
				this.path = path;
				this.arrivalTime = arrivalTime;
				this.priority = priority;
			}
		}
	}

	/**
	 * Two-column table model for viewing the legacy memory snapshot.
	 */
	private static final class MemoryTableModel extends AbstractTableModel {
		private static final String[] COLUMNS = {"Index", "Value"};
		private Object[] memory = new Object[0];

		@Override
		public int getRowCount() {
			return memory.length;
		}

		@Override
		public int getColumnCount() {
			return COLUMNS.length;
		}

		@Override
		public String getColumnName(int column) {
			return COLUMNS[column];
		}

		@Override
		public Class<?> getColumnClass(int columnIndex) {
			return columnIndex == 0 ? Integer.class : String.class;
		}

		@Override
		public boolean isCellEditable(int rowIndex, int columnIndex) {
			return false;
		}

		@Override
		public Object getValueAt(int rowIndex, int columnIndex) {
			if (columnIndex == 0) return rowIndex;
			return String.valueOf(memory[rowIndex]);
		}

		void setMemory(Object[] memory) {
			this.memory = memory == null ? new Object[0] : memory;
			fireTableDataChanged();
		}
	}

	/**
	 * Summary table for the realistic scheduler mode.
	 */
	private static final class RealisticSummaryTableModel extends AbstractTableModel {
		private static final String[] COLUMNS = {"PID", "Arrival", "Start", "Finish", "Turnaround", "Response", "State", "Variables"};

		private List<SimProcess> processes = List.of();
		@SuppressWarnings("unused")
		private int endTime = 0;

		@Override
		public int getRowCount() {
			return processes.size();
		}

		@Override
		public int getColumnCount() {
			return COLUMNS.length;
		}

		@Override
		public String getColumnName(int column) {
			return COLUMNS[column];
		}

		@Override
		public Object getValueAt(int rowIndex, int columnIndex) {
			SimProcess p = processes.get(rowIndex);
			Integer start = p.firstRunTime;
			Integer finish = p.finishTime;
			Integer turnaround = finish == null ? null : (finish - p.arrivalTime);
			Integer response = start == null ? null : (start - p.arrivalTime);

			return switch (columnIndex) {
				case 0 -> p.pid;
				case 1 -> p.arrivalTime;
				case 2 -> start;
				case 3 -> finish;
				case 4 -> turnaround;
				case 5 -> response;
				case 6 -> p.state;
				case 7 -> p.variables.toString();
				default -> null;
			};
		}

		void setProcesses(List<SimProcess> processes, int endTime) {
			this.processes = processes == null ? List.of() : processes;
			this.endTime = endTime;
			fireTableDataChanged();
		}
	}

	/**
	 * Table model for the structured trace events emitted by {@link RealisticScheduler}.
	 */
	private static final class EventsTableModel extends AbstractTableModel {
		private static final String[] COLUMNS = {"Time", "PID", "Type", "Message"};
		private List<SimulationEvent> events = List.of();

		@Override
		public int getRowCount() {
			return events.size();
		}

		@Override
		public int getColumnCount() {
			return COLUMNS.length;
		}

		@Override
		public String getColumnName(int column) {
			return COLUMNS[column];
		}

		@Override
		public Object getValueAt(int rowIndex, int columnIndex) {
			SimulationEvent e = events.get(rowIndex);
			return switch (columnIndex) {
				case 0 -> e.time;
				case 1 -> e.pid;
				case 2 -> e.type;
				case 3 -> e.message;
				default -> null;
			};
		}

		void setEvents(List<SimulationEvent> events) {
			this.events = events == null ? List.of() : events;
			fireTableDataChanged();
		}

		String toCsv() {
			StringBuilder sb = new StringBuilder();
			sb.append("time,pid,type,message\n");
			for (SimulationEvent e : events) {
				sb.append(e.time).append(',');
				sb.append(e.pid == null ? "" : e.pid).append(',');
				sb.append(e.type).append(',');
				sb.append(csvEscape(e.message)).append('\n');
			}
			return sb.toString();
		}

		private static String csvEscape(String s) {
			if (s == null) return "";
			boolean needsQuotes = s.contains(",") || s.contains("\"") || s.contains("\n") || s.contains("\r");
			String escaped = s.replace("\"", "\"\"");
			return needsQuotes ? "\"" + escaped + "\"" : escaped;
		}
	}
}
