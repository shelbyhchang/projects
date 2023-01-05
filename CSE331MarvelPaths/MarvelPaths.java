package marvel;

import graph.*;

import java.io.IOException;
import java.util.*;

/**
 * This class represents a path finder between Marvel characters.
 */

public class MarvelPaths {

    // This class is not an ADT.

    public static void main(String[] args) throws IOException {
        Scanner input = new Scanner(System.in);
        System.out.print("Welcome to the Marvel Path Finder, where we find the shortest path between any two ");
        System.out.println("Marvel characters of your choice.");
        System.out.println("Please make sure to enter the full name with no unnecessary spaces or characters.");
        System.out.println("** If you wish to quit at any point, simply type \"quit\".");
        System.out.println("\nExamples of acceptable names:");
        System.out.println("Old Skull, old skull, Old skull, 3-D mAn, CHARLES CHAN, 3-d man/Charles Chan");
        System.out.println("Examples of unacceptable names:");
        System.out.println("old skul, OLD-SKULL, Old   Skull  , 3-dMan, 3D Man");
        System.out.print("\nEnter the first Marvel character: ");
        String name1 = input.nextLine();
        if (name1.equals("quit")) {
            System.out.println("Have a nice day.");
            input.close();
            return;
        }
        System.out.print("Enter the second Marvel character: ");
        String name2 = input.nextLine();
        if (name2.equals("quit")) {
            System.out.println("Have a nice day.");
            input.close();
            return;
        }

        // Formats the user inputted names
        String char1 = formatName(name1);
        String char2 = formatName(name2);

        // Prints out the path, or alternate output if the characters do not exist
        Graph<String,String> marvel = buildGraph("marvel.csv");
        if (!marvel.containsNode(char1) || !marvel.containsNode(char2)) {
            if (!marvel.containsNode(char1)) {
                System.out.println("unknown: " + char1);
            }
            if (!marvel.containsNode(char2)) {
                System.out.println("unknown: " + char2);
            }
        } else {
            List<Graph.Edge<String,String>> path = findPath(marvel, char1, char2);
            String result = formatPath(marvel, path, char1, char2);
            System.out.println(result);
        }

        // Asks if user wants to find another path
        System.out.print("Find another Marvel path (Y/N) : ");
        String again = input.next();
        if (again.equalsIgnoreCase("Y")) {
            main(null); // if input is Y then call main again.
        } else {
            System.out.println("Have a nice day.");
            input.close();
        }
    }

    /**
     * Private helper method that formats user-inputted names into style that matches names of characters in marvel.csv
     * @param name user-inputted name
     * @return correctly formatted name that matches the user-inputted name
     * @throws NullPointerException if name is null
     * @throws IOException if an error occurs while reading the marvel.csv file
     */
    private static String formatName(String name) throws IOException {
        if (name == null) {
            throw new NullPointerException("name is null");
        }

        String original = name.toUpperCase();
        // converts any spaces to dashes (example: 3-D MAN --> 3-D-MAN)
        if (original.contains(" ")) {
            String fixed = "";
            for (String section : original.split(" ")) {
                fixed = fixed + section + "-";
            }
            original = fixed.substring(0, fixed.length() - 1); // Removes extra "-" at the end
        }
        Graph<String, String> graph = buildGraph("marvel.csv");
        // Finds full character name (example: 3D-MAN --> 3-D-MAN/CHARLES-CHAN)
        for (String character : graph.getNodes()) {
            if (character.contains("/")) { // example: 3-D-MAN/CHARLES-CHAN
                for (String partial : character.split("/")) {
                    if (partial.equals(original)) {
                        return character;
                    }
                }
            } else if (character.equals(original)) {
                return character;
            }
        }
        return original;
    }

    /**
     * Private helper method that reorganizes a given path into a more clear fashion for the reader to see easily
     * @param path the path from char1 to char2 that is to be properly formatted
     * @param char1 the starting character for the path
     * @param char2 the destination character for the path
     * @return a String representing the path in more legible fashion
     * @throws NullPointerException if the graph, path, char1, or char2 are null
     */
    private static String formatPath(Graph<String,String> graph, List<Graph.Edge<String,String>> path, String char1, String char2) {
        if (graph == null) {
            throw new NullPointerException("graph is null");
        }
        if (path == null) {
            throw new NullPointerException("path is null");
        }
        if (char1 == null) {
            throw new NullPointerException("char1 is null");
        }
        if (char2 == null) {
            throw new NullPointerException("char2 is null");
        }

        String result = "Path from " + char1 + " to " + char2 + ":";
        if (path.isEmpty() && !char1.equals(char2)) {
            result += "\nno path found";
        } else {
            for (Graph.Edge<String,String> edge : path) {
                String charA = edge.getStart();
                String charB = edge.getDest();
                String book = edge.getLabel();
                result = result + "\n" + charA + " to " + charB + " via " + book;
            }
        }
        return result;
    }

    /**
     * Creates a graph representing Marvel characters and relationships between them
     * @param filename the file containing raw data about Marvel characters and books they appear in
     * @throws NullPointerException if filename is null
     * @throws IOException if an error occurs while reading the file
     * @return a Graph representing Marvel characters and relationships between them
     * @spec.requires filename is a valid file in the resources/data folder.
     * @spec.effects Creates a graph representing Marvel characters and relationships between them
     */
    public static Graph<String,String> buildGraph(String filename) throws IOException {
        if (filename == null)
            throw new NullPointerException("filename is null");
        Graph<String,String> graph = new Graph<>();
        Map<String, List<String>> data = MarvelParser.parseData(filename);
        for (String book : data.keySet()) {
            // Create nodes for all characters in this book, if not already present
            for (String character : data.get(book)) {
                if (!graph.containsNode(character)) {
                    graph.addNode(character);
                }
            }

            // Create all edges between characters for this book
            for (int i = 0; i < data.get(book).size() - 1; i++) {
                String charA = data.get(book).get(i);
                int j = i + 1;

                while (j < data.get(book).size()) {
                    String charB = data.get(book).get(j);

                    // Create first edge from Character A to Character B
                    Graph.Edge<String,String> edge1 = new Graph.Edge<>(charA, charB, book);

                    // Create second edge from Character B to Character A
                    Graph.Edge<String,String> edge2 = new Graph.Edge<>(charB, charA, book);
                    graph.addEdge(edge1);
                    graph.addEdge(edge2);

                    j++;
                }
            }
        }
        return graph;
    }

    /**
     * A BFS algorithm that finds the shortest path from a starting node to a destination node
     * @param graph the graph where the path is being searched for
     * @param start the starting node
     * @param dest the destination node
     * @return the shortest path from start to dest, or an empty list if there is no path from start to dest
     * @throws NullPointerException if graph, start, or dest is null
     * @spec.requires The provided graph is not null
     * @spec.requires Both the starting character and the destination character are not null and are in the graph
     */
    public static List<Graph.Edge<String,String>> findPath(Graph<String,String> graph, String start, String dest) {
        if (graph == null)
            throw new NullPointerException("graph is null");
        if (start == null || dest == null)
            throw new NullPointerException("starting character or destination character is null");

        // Represents queue of nodes to visit
        Queue<String> queue = new LinkedList<>();
        // Tracks visited nodes and their paths (keys: nodes, values: paths)
        Map<String, List<Graph.Edge<String,String>>> tracker = new HashMap<>();
        queue.add(start);
        tracker.put(start, new ArrayList<>());

        while (!queue.isEmpty()) {
            String next = queue.remove();

            if (next.equals(dest)) {
                return Collections.unmodifiableList(tracker.get(next));
            }

            List<Graph.Edge<String,String>> current = tracker.get(next); // current: the path followed from start to next
            Set<Graph.Edge<String,String>> edges = graph.getOutEdges(next); // edges: the outgoing edges from next
            for (Graph.Edge<String,String> edge : sortEdges(edges)) {
                String child = edge.getDest();

                // Verifies that child has not been visited yet
                if (!tracker.containsKey(child)) {
                    List<Graph.Edge<String,String>> path = new ArrayList<>(current);
                    path.add(edge);
                    tracker.put(child, path);
                    String copyOfChild = new String(child);
                    queue.add(copyOfChild);
                }
            }
        }

        // If the code has reached this point, then the loop has been terminated, and no path exists.
        return new ArrayList<>();
    }

    /**
     * Private helper method that sorts a given collection of edges by lexicographical order,
     *  sorted first by character name, then by book name if character names are equal.
     * @param edges the given collection of edges to be sorted
     * @return a sorted collection of edges
     * @throws NullPointerException if edges is null
     */
    private static List<Graph.Edge<String,String>> sortEdges(Set<Graph.Edge<String,String>> edges) {
        if (edges == null) {
            throw new NullPointerException("edges is null");
        }

        List<Graph.Edge<String,String>> copy = new ArrayList<>(edges);

        class Sort implements Comparator<Graph.Edge<String,String>> {
            public int compare(Graph.Edge<String,String> edge1, Graph.Edge<String,String> edge2) {
                String child1 = edge1.getDest();
                String child2 = edge2.getLabel();
                String book1 = edge1.getDest();
                String book2 = edge2.getLabel();
                if(!(child1.equals(child2)))
                    return child1.compareTo(child2);

                if (!(book1.equals(book2)))
                    return book1.compareTo(book2);

                return 0;
            }
        }

        Collections.sort(copy, new Sort());
        return copy;
    }

}
