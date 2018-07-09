import ru.myx.ae1.AcmPluginFactory;
import ru.myx.ae1.PluginInstance;
import ru.myx.ae3.base.BaseObject;
import ru.myx.al.api.access.Plugin;

/*
 * Created on 07.10.2003
 *
 * To change the template for this generated file go to
 * Window>Preferences>Java>Code Generation>Code and Comments
 */
final class PluginFactory implements AcmPluginFactory {
	
	private static final String[] VARIETY = {
			"ACMMOD:USER_MANAGER", "mwm.plugins.UserManagement.plugin"
	};

	@Override
	public final PluginInstance produce(final String variant, final BaseObject attributes, final Object source) {
		
		return new Plugin();
	}

	@Override
	public final String[] variety() {
		
		return PluginFactory.VARIETY;
	}
}
