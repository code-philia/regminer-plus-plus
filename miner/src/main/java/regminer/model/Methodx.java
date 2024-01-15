package regminer.model;

import org.eclipse.jdt.core.dom.MethodDeclaration;

/**
 * @author sxz
 * 该类进一步的封装了MethodDeclaration
 */
public class Methodx {

	private String signature;
	private int startLine;
	private int stopLine;
	private String simpleName;
	private String filePath;
	private MethodDeclaration methodDeclaration;

	public Methodx(String signature) {
		this.signature = signature;
	}

	public Methodx(String signature, String filePath) {
		this.signature = signature;
		this.filePath = filePath;
	}

	public Methodx(String signature, int startLine, int stopLine, String simpleName,
			MethodDeclaration methodDeclaration) {
		this.signature = signature;
		this.startLine = startLine;
		this.stopLine = stopLine;
		this.simpleName = simpleName;
		this.methodDeclaration = methodDeclaration;
	}

	public String getFilePath() {
		return filePath;
	}

	public String getSignature() {
		return signature;
	}

	public void setSignature(String signature) {
		this.signature = signature;
	}

	public int getStartLine() {
		return startLine;
	}

	public void setStartLine(int startLine) {
		this.startLine = startLine;
	}

	public int getStopLine() {
		return stopLine;
	}

	public void setStopLine(int stopLine) {
		this.stopLine = stopLine;
	}

	public String getSimpleName() {
		return simpleName;
	}

	public void setSimpleName(String simpleName) {
		this.simpleName = simpleName;
	}

	public MethodDeclaration getMethodDeclaration() {
		return methodDeclaration;
	}

	public void setMethodDeclaration(MethodDeclaration methodDeclaration) {
		this.methodDeclaration = methodDeclaration;
	}

	@Override
	public int hashCode() {
		return (this.filePath + this.signature + this.startLine + this.stopLine).hashCode();
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}

		if (o == null) {
			return false;
		}

		if (o instanceof Methodx) {
			Methodx methodx = (Methodx) o;
			return this.signature.equals(methodx.getSignature()) &&
					this.filePath.equals(methodx.getFilePath()) &&
					this.startLine == methodx.getStartLine() &&
					this.stopLine == methodx.getStopLine();
		}
		return false;
	}
}
