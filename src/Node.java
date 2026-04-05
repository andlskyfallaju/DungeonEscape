public class Node {

    int col, row;
    int gCost, hCost, fCost;
    Node parent;

    public Node(int col, int row) {
        this.col = col;
        this.row = row;
    }

    public void calculateFCost() {
        fCost = gCost + hCost;
    }
}