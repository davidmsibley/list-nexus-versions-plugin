package gov.usgs.cida.jenkins;

import hudson.EnvVars;
import hudson.model.AbstractBuild;
import hudson.model.ParameterValue;
import hudson.util.VariableResolver;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.export.Exported;

/**
 *
 * @author dmsibley
 */
public class ListNexusVersionsParameterValue extends ParameterValue {

	/** suid. */
	private static final long serialVersionUID = 3503457375763602613L;
	@Exported(visibility = 3)
	private String version;

	@DataBoundConstructor
	public ListNexusVersionsParameterValue(String name, String version) {
		super(name);
		this.version = version;
	}

	@Override
	public void buildEnvVars(AbstractBuild<?, ?> build, EnvVars env) {
		env.put(this.getName(), this.getVersion());
	}

	@Override
	public VariableResolver<String> createVariableResolver(AbstractBuild<?, ?> build) {
		return new VariableResolver<String>() {

			public String resolve(String name) {
				return ListNexusVersionsParameterValue.this.name.equals(name) ? getVersion() : null;
			}
		};
	}

	public String getVersion() {
		return this.version;
	}

	public void setVersion(String version) {
		this.version = version;
	}

	@Override
	public String toString() {
		return "(ListNexusVersionsParameterValue) " + this.getName() + ": Version='" + this.getVersion() + "'";
	}
}
