package be.kuleuven.rega.phylogeotool.io.read;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import jebl.evolution.io.ImportException;
import jebl.evolution.io.NewickImporter;
import jebl.evolution.io.NexusImporter;
import jebl.evolution.io.TreeImporter;
import jebl.evolution.trees.SimpleRootedTree;
import jebl.evolution.trees.Tree;
import be.kuleuven.rega.phylogeotool.core.Edge;
import be.kuleuven.rega.phylogeotool.core.Node;

public class ReadTree {
	
	private static Tree jeblTree;
	private static be.kuleuven.rega.phylogeotool.core.Tree treeDrawTree;
	
	// TODO: Improve the code to distinguish between method that stores the tree and one that creates new tree
	public static void setJeblTree(String locationJeblTree) {
		if(jeblTree == null) {
//			Settings settings = Settings.getInstance();
			try {
				jeblTree = readTree(new FileReader(new File(locationJeblTree)));
			} catch (FileNotFoundException e) {
				System.err.println("Could not find the tree to import.");
				e.printStackTrace();
			}
		}
	}
	
	public static Tree getJeblTree() {
		if(jeblTree != null) {
			return jeblTree;
		} else {
			throw new RuntimeException("Jebl Tree not set before trying to get!");
		}
	}
	
	public static be.kuleuven.rega.phylogeotool.core.Tree setTreeDrawTree(Tree jeblTree) {
		if(treeDrawTree == null) {
			System.err.println("Reading Draw Tree");
			treeDrawTree = jeblToTreeDraw((SimpleRootedTree)jeblTree, null);
		}
		return treeDrawTree;
	}
	
	public static be.kuleuven.rega.phylogeotool.core.Tree getTreeDrawTree() {
		if(treeDrawTree != null) {
			return treeDrawTree;
		} else {
			throw new RuntimeException("TreeDraw Tree not set before trying to get!");
		}
	}
	
	public static Tree readTree(Reader reader) {
		BufferedReader bufferedReader = new BufferedReader(reader);
		String line;
		List<Tree> trees = new ArrayList<Tree>();
		StringBuilder fullText = new StringBuilder();
		try {
			while ((line = bufferedReader.readLine()) != null) {
				fullText.append(line + "\n");
			} 
			String tree = fullText.toString();
			boolean isNexus;
        	if(tree.contains("#NEXUS")) {
        		isNexus = true;
        	} else {
        		isNexus = false;
        	}
			TreeImporter importer;
			if (isNexus) {
                importer = new NexusImporter(new StringReader(tree));
            } else {
                importer = new NewickImporter(new StringReader(tree), true);
            }		
			trees.add(importer.importNextTree());

			bufferedReader.close();
			if (trees.size() == 0) {
				throw new ImportException("This file contained no trees.");
			} else {
				return trees.get(0);
			}
		} catch (IOException e) {
			System.err.println(ReadTree.class.getName() + ": Treefile not found");
			e.printStackTrace();
		} catch (ImportException e) {
			System.err.println(ReadTree.class.getName() + ": Could not import the tree");
		}
        
		return null;
	}
	
	public static be.kuleuven.rega.phylogeotool.core.Tree jeblToTreeDraw(SimpleRootedTree tree, List<String> pplacerSequences) {
		List<Node> nodes = new ArrayList<Node>();
		List<Edge> edges = new ArrayList<Edge>();
		i = 1;
		if(pplacerSequences == null) {
			pplacerSequences = new ArrayList<String>();
		} else {
			nrNodes = tree.getNodes().size() - (2*pplacerSequences.size() - 1);
		}
		preOrder(tree, tree.getRootNode(), null, nodes, edges, pplacerSequences);
		return new be.kuleuven.rega.phylogeotool.core.Tree(new HashSet<Node>(nodes), new HashSet<Edge>(edges), nodes.iterator().next());
	}
	
	/**
	 * 1.  Print out the root's value, regardless of whether you are at the actual root or 
	 * 	   just the subtree's root.
	 * 2.  Go to the left child node, and then perform a pre-order 
	 * 	   traversal on that left child node's subtree.
	 * 3.  Go to the right child node, and then perform a 
	 * 	   pre-order traversal on that right child node's subtree.
	 * 4.  Do this recursively.
	 * @param simpleRootedTree
	 * @param root
	 */
	private static int i = 1;
	private static int nrNodes;
	private static void preOrder (jebl.evolution.trees.RootedTree tree, jebl.evolution.graphs.Node root, Node parent, List<Node> nodes, List<Edge> edges, List<String> pplacerSequences) {
		if(root == null) return;
		
		Node tempStorage = null;
		String attribute = null;
		
		if(root.getAttribute("!name") != null && !root.getAttribute("!name").equals("")) {
			attribute = root.getAttribute("!name").toString();
		}
		
		if(tree.getChildren(root).size() <= 0) {
			/* Special pplacer situation */
			if(pplacerSequences.contains(tree.getTaxon(root).getName())) {
				tempStorage = new Node(nrNodes++, tree.getTaxon(root).getName(), parent, attribute);
			} else {
				tempStorage = new Node(i++, tree.getTaxon(root).getName(), parent, attribute);
			}
		} else {
			boolean pplacedChild = false;
			
			for(jebl.evolution.graphs.Node node:tree.getChildren(root)) {
				if(tree.getChildren(node).size() <= 0 && pplacerSequences.contains(tree.getTaxon(node).toString())) {
					pplacedChild = true;
				}
			}
			if(pplacedChild) {
				tempStorage = new Node(nrNodes++, "", parent, attribute);
			} else {
				tempStorage = new Node(i++, "", parent, attribute);
			}
		}
		
		nodes.add(tempStorage);
		if(parent != null) {
			edges.add(new Edge("", parent, tempStorage, tree.getLength(root)));
		}
		
		if(tree.getChildren(root).size() > 0) {
			for(int j = tree.getChildren(root).size() - 1; j >= 0; j-- ) {
				preOrder(tree, tree.getChildren(root).get(j), tempStorage, nodes, edges, pplacerSequences);
			}
		} else {
			preOrder(tree, null, tempStorage, nodes, edges, pplacerSequences);
		}
	}
}
