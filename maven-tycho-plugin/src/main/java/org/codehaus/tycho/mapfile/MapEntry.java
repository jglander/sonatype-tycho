package org.codehaus.tycho.mapfile;

public class MapEntry {

	private String kind;

	private String name;

	private String scmPath;

	private String scmUrl;

	private String version;

	public MapEntry() {
		super();
	}

	public MapEntry(String kind, String name, String version, String scmURL,
			String scmPath) {
		super();
		this.kind = kind;
		this.name = name;
		this.version = version;
		this.scmUrl = scmURL;
		this.scmPath = scmPath;
	}

	public String getKind() {
		return kind;
	}

	public String getName() {
		return name;
	}

	public String getScmPath() {
		return scmPath;
	}

	public String getScmUrl() {
		return scmUrl;
	}

	public String getVersion() {
		return version;
	}

	public void setKind(String kind) {
		this.kind = kind;
	}

	public void setName(String name) {
		this.name = name;
	}

	public void setScmPath(String scmPath) {
		this.scmPath = scmPath;
	}

	public void setScmUrl(String scmUrl) {
		this.scmUrl = scmUrl;
	}

	public void setVersion(String version) {
		this.version = version;
	}
}
