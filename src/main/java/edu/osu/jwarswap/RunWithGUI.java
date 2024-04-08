package edu.osu.jwarswap;
import javax.swing.*;
import java.awt.event.*;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.TextField;
import java.awt.Button;
import java.awt.TextArea;
import java.awt.Font;
import java.awt.Component;
import java.awt.Panel;

public class RunWithGUI extends JFrame{
	JFrame frame;
	private final ButtonGroup buttonGroup = new ButtonGroup();
	private JTextField txtVertexcoloringexampletsv;
	RunWithGUI(){
		
		JTabbedPane mainTabbedPane = new JTabbedPane(JTabbedPane.TOP);
		getContentPane().add(mainTabbedPane, BorderLayout.NORTH);
		
		JPanel setupPanel = new JPanel();
		setupPanel.setBackground(new Color(255, 255, 255));
		mainTabbedPane.addTab("Setup", null, setupPanel, null);
		setupPanel.setLayout(new BoxLayout(setupPanel, BoxLayout.X_AXIS));
		
		Box verticalBox = Box.createVerticalBox();
		setupPanel.add(verticalBox);
		
		Box edgelistSelectVerticalBox = Box.createVerticalBox();
		verticalBox.add(edgelistSelectVerticalBox);
		
		JTextArea txtrSelectAnEdgelist = new JTextArea();
		txtrSelectAnEdgelist.setText("Select an edgelist file from your disk.");
		edgelistSelectVerticalBox.add(txtrSelectAnEdgelist);
		
		Box edgeListSelectInnerHorizontalBox = Box.createHorizontalBox();
		verticalBox.add(edgeListSelectInnerHorizontalBox);
		
		TextField edgelistSelectTextField = new TextField();
		edgeListSelectInnerHorizontalBox.add(edgelistSelectTextField);
		edgelistSelectTextField.setText("example.tsv");
		
		JButton btnBrowseEdgelist = new JButton("Browse...");
		edgeListSelectInnerHorizontalBox.add(btnBrowseEdgelist);
		
		Box randomizationOptionsHorizontalBox = Box.createHorizontalBox();
		verticalBox.add(randomizationOptionsHorizontalBox);
		
		JTextArea txtrRandomizationOptions = new JTextArea();
		randomizationOptionsHorizontalBox.add(txtrRandomizationOptions);
		txtrRandomizationOptions.setEditable(false);
		txtrRandomizationOptions.setFont(new Font("Dialog", Font.PLAIN, 12));
		txtrRandomizationOptions.setText("Randomization Options:");
		
		JCheckBox chckbxSelfLoops = new JCheckBox("Allow Self-Loops");
		randomizationOptionsHorizontalBox.add(chckbxSelfLoops);
		
		JCheckBox chckbxColoredEdges = new JCheckBox("Use Colored Edges");
		chckbxColoredEdges.setHorizontalAlignment(SwingConstants.CENTER);
		randomizationOptionsHorizontalBox.add(chckbxColoredEdges);
		
		JCheckBox chckbxColoredVertices = new JCheckBox("New check box");
		randomizationOptionsHorizontalBox.add(chckbxColoredVertices);
		
		Box graphColoringHorizontalBox = Box.createHorizontalBox();
		verticalBox.add(graphColoringHorizontalBox);
		
		JTextArea txtrGraphColoringOptions_1 = new JTextArea();
		txtrGraphColoringOptions_1.setText("Graph Coloring Options:");
		graphColoringHorizontalBox.add(txtrGraphColoringOptions_1);
		
		JRadioButton rdbtnNoColoring = new JRadioButton("No coloring");
		buttonGroup.add(rdbtnNoColoring);
		graphColoringHorizontalBox.add(rdbtnNoColoring);
		
		JRadioButton rdbtnuseColoredVertices = new JRadioButton("Use Colored Vertices");
		buttonGroup.add(rdbtnuseColoredVertices);
		graphColoringHorizontalBox.add(rdbtnuseColoredVertices);
		
		JRadioButton rdbtnUseColoredEdges = new JRadioButton("Use Colored Edges");
		buttonGroup.add(rdbtnUseColoredEdges);
		graphColoringHorizontalBox.add(rdbtnUseColoredEdges);
		
		Box vertexColorFileHorizontalBox = Box.createHorizontalBox();
		verticalBox.add(vertexColorFileHorizontalBox);
		
		txtVertexcoloringexampletsv = new JTextField();
		vertexColorFileHorizontalBox.add(txtVertexcoloringexampletsv);
		txtVertexcoloringexampletsv.setFont(new Font("Dialog", Font.PLAIN, 14));
		txtVertexcoloringexampletsv.setEnabled(false);
		txtVertexcoloringexampletsv.setText("vertex_coloring_example.tsv");
		txtVertexcoloringexampletsv.setColumns(10);
		
		JButton btnVertexColoringBrowse = new JButton("Browse...");
		btnVertexColoringBrowse.setEnabled(false);
		vertexColorFileHorizontalBox.add(btnVertexColoringBrowse);
		
		Box startHorizontalBox = Box.createHorizontalBox();
		verticalBox.add(startHorizontalBox);
		
		JButton btnStartButton = new JButton("Generate Random Sample");
		startHorizontalBox.add(btnStartButton);
		
		Box horizontalBox = Box.createHorizontalBox();
		verticalBox.add(horizontalBox);
		
		JProgressBar progressBar = new JProgressBar();
		horizontalBox.add(progressBar);
		
		JTextArea txtrOutOf = new JTextArea();
		txtrOutOf.setText("0 out of ??? random graphs generated.");
		horizontalBox.add(txtrOutOf);
		
		JPanel motifsPanel = new JPanel();
		mainTabbedPane.addTab("Motifs", null, motifsPanel, null);
		motifsPanel.setLayout(new BorderLayout(0, 0));
		
		Box motifsMainBox = Box.createVerticalBox();
		motifsPanel.add(motifsMainBox, BorderLayout.NORTH);
		
		Box horizontalBox_1 = Box.createHorizontalBox();
		motifsMainBox.add(horizontalBox_1);
		
		JTextArea txtrSelect = new JTextArea();
		horizontalBox_1.add(txtrSelect);
		txtrSelect.setText("Motif size (number of vertices):");
		
		JSpinner spinner = new JSpinner();
		horizontalBox_1.add(spinner);
		
		Box enumerateSubgraphsButtonBox = Box.createHorizontalBox();
		motifsMainBox.add(enumerateSubgraphsButtonBox);
		
		JButton enumerateSubgraphsButton = new JButton("Enumerate Subgraphs");
		enumerateSubgraphsButton.setActionCommand("");
		enumerateSubgraphsButtonBox.add(enumerateSubgraphsButton);
		
		JScrollPane motifsScrollPane = new JScrollPane();
		motifsMainBox.add(motifsScrollPane);
		
		JPanel otherPanel = new JPanel();
		mainTabbedPane.addTab("Other Results", null, otherPanel, null);
		
		JPanel helpPanel = new JPanel();
		mainTabbedPane.addTab("Help", null, helpPanel, null);
		
		Box helpPanelMainBox = Box.createVerticalBox();
		helpPanel.add(helpPanelMainBox);
		
		JTextArea txtrThisPanelWill = new JTextArea();
		helpPanelMainBox.add(txtrThisPanelWill);
		txtrThisPanelWill.setText("This panel will have an overview of the steps needed to test a netowork motif hypothesis, and some sub-panels that can be expanded for more detailed instructions and recommendations.");
		frame = new JFrame();
		
	}
}


class MainTabbedPane {
	
}