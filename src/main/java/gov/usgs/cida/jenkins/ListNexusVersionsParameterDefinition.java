package gov.usgs.cida.jenkins;

import hudson.Extension;
import hudson.model.ParameterDefinition;
import hudson.model.ParameterValue;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.UUID;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import net.sf.json.JSONObject;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.log4j.Logger;
import org.apache.maven.artifact.versioning.ComparableVersion;
import org.codehaus.plexus.util.StringUtils;
import org.jvnet.localizer.ResourceBundleHolder;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.StaplerRequest;

/**
 *
 * @author dmsibley
 */
public class ListNexusVersionsParameterDefinition extends ParameterDefinition implements Comparable<ListNexusVersionsParameterDefinition> {
	/** SUID. */
	private static final long serialVersionUID = -3502280860537289034L;

	private static final Logger LOGGER = Logger.getLogger(ListNexusVersionsParameterDefinition.class);
	
	private final UUID uuid;
	private final String projId;
	private final String repo;
	private final Boolean releasedOnly;

	@DataBoundConstructor
	public ListNexusVersionsParameterDefinition(String name, String repo, String projId, String uuid, Boolean releasedOnly) {
		super(name, ResourceBundleHolder.get(ListNexusVersionsParameterDefinition.class).format("TagDescription"));
		
		this.repo = repo;
		this.projId = projId;
		this.releasedOnly = releasedOnly;
		
		if (uuid == null || uuid.length() == 0) {
			this.uuid = UUID.randomUUID();
		} else {
			this.uuid = UUID.fromString(uuid);
		}
	}

	@Override
	public ParameterValue createValue(StaplerRequest sr) {
		String[] values = sr.getParameterValues(getName());
		if (values == null || values.length != 1) {
			return this.getDefaultParameterValue();
		}
		return new ListNexusVersionsParameterValue(getName(), values[0]);
	}

	@Override
	public ParameterValue createValue(StaplerRequest sr, JSONObject jsono) {
		ListNexusVersionsParameterValue result = sr.bindJSON(ListNexusVersionsParameterValue.class, jsono);
		return result;
	}

	@Override
	public ParameterValue getDefaultParameterValue() {
		String result = "";
		List<String> versions = getVersions();
		
		if (0 < versions.size()) {
			result = versions.get(0);
		}
		
		return new ListNexusVersionsParameterValue(getName(), result);
	}

	@Override
	public DescriptorImpl getDescriptor() {
		return (DescriptorImpl) super.getDescriptor();
	}

	public String getRepo() {
		return this.repo;
	}
	
	public String getProjId() {
		return this.projId;
	}
	
	public Boolean getReleasedOnly() {
		return this.releasedOnly;
	}
	
	/**
	 * Returns a list of artifact versions to be displayed in
	 * {@code ListNexusVersionsParameterDefinition/index.jelly}.
	 *
	 * @return
	 */
	public List<String> getVersions() {
		Map<ComparableVersion, String> result = new TreeMap<ComparableVersion, String>();
		
		String[] artifactName = this.projId.split(":");
		String uri = this.repo + "/service/local/lucene/search?g=" + artifactName[0] + "&a=" + artifactName[1];
		
		GetMethod GET = new GetMethod(uri);
		HttpClient httpClient = new HttpClient();
		try {
			GET.setFollowRedirects(true);
			int code = httpClient.executeMethod(GET);
			if (200 == code) {
				InputStream in = GET.getResponseBodyAsStream();
				XMLStreamReader xml = null;
				try {
					xml = XMLInputFactory.newInstance().createXMLStreamReader(in);

					while (xml.hasNext()) {
						int tag = xml.next();
						if (XMLStreamReader.START_ELEMENT == tag && "artifact".equals(xml.getLocalName())) {
							while (xml.hasNext()) {
								int inner = xml.next();
								if (XMLStreamReader.START_ELEMENT == inner && "version".equals(xml.getLocalName())) {
									String text = xml.getElementText();
									if (!this.releasedOnly || !StringUtils.contains(StringUtils.upperCase(text), "-SNAPSHOT")) {
										result.put(new ComparableVersion(text), text);
									}
								}
							}
						}
					}

				} finally {
					if (null != xml) {
						xml.close();
					}
				}
			}
		} catch (IOException e) {
			LOGGER.error(null, e);
		} catch (XMLStreamException e) {
			LOGGER.error(null, e);
		} finally {
			GET.releaseConnection();
		}
		List<String> vals = new ArrayList<String>(result.values()); 
		Collections.reverse(vals);
		return vals;
	}
	
	public int compareTo(ListNexusVersionsParameterDefinition o) {
		int result = -1;
		if (null != o && o.uuid.equals(this.uuid)) {
			result = 0;
		}
		return result;
	}

	@Extension
	public static class DescriptorImpl extends ParameterDescriptor {

		@Override
		public String getDisplayName() {
			return ResourceBundleHolder.get(ListNexusVersionsParameterDefinition.class).format("DisplayName");
		}
	}
}
