import java.io.Writer;
import java.io.IOException;

public class CeylonTreePrinter extends CeylonTree.Visitor {
  private Writer out;
  private int depth;

  public CeylonTreePrinter(Writer out) {
    this.out = out;
  }

  private void print(String str) {
    try {
      out.append(str);
    }
    catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  private void newline() {
    print("\n");
  }

  private void indent() {
    for (int i = 0; i < depth; i++)
      print("  ");
  }

  private String getName(CeylonTree tree) {
    String name = tree.getClass().getName();
    return name.substring(name.indexOf('$') + 1);
  }
  
  public void visitDefault(CeylonTree tree) {
    int child_count = tree.children.size();
    if (child_count != 0) {
      newline();
      indent();
      print("(" + getName(tree));
      depth++;
      for (CeylonTree child : tree.children) {
        child.accept(this);
      }
      print(")");
      depth--;
    }
    else {
      print(" " + getName(tree));
    }
  }
}
