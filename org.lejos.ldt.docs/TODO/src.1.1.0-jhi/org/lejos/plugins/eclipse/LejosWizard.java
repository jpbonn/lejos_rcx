package org.lejos.plugins.eclipse;

import java.io.File;

import org.eclipse.core.runtime.Path;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.internal.ui.wizards.NewProjectCreationWizard;
import org.eclipse.jdt.ui.wizards.JavaCapabilityConfigurationPage;
import org.lejos.plugins.eclipse.util.FileUtilities;

/**
 * This is rather a hack for eclipse 2.0 (using internal class)
 * @see Wizard
 */
public class LejosWizard extends NewProjectCreationWizard {
// this code makes use of eclipse 2.0 internals

	
	/*
	 * @see Wizard#performFinish
	 */		
	public boolean performFinish() {
		boolean res=super.performFinish();
		
		// updating classspath
		customize();
		
		return res;
	}
		    
	// customize the classpath for compiling !
    private void customize() {

        try {
  			JavaCapabilityConfigurationPage page=(JavaCapabilityConfigurationPage)getPage("JavaCapabilityConfigurationPage");
			if (page!=null) {
				IJavaProject prj=page.getJavaProject();				
				IClasspathEntry[] ocp=prj.getRawClasspath();
				IClasspathEntry[] ncp=new IClasspathEntry[ocp.length+3];
				for(int i=0; i<ocp.length; i++) ncp[i]=ocp[i];
				if (ocp.length>0) ncp[ncp.length-1]=ocp[ocp.length-1];

				// JVM libs
				String home=LejosPlugin.getDefault().getLejosPath();
				File fp=new File(home,"lib/classes.jar");
				Path cp=new Path(fp.getCanonicalPath());
				IClasspathEntry icp=JavaCore.newLibraryEntry(cp,null,null);
				ncp[ncp.length-4]=icp;

				fp=new File(home,"lib/rcxrcxcomm.jar");
				cp=new Path(FileUtilities.getAbsolutePath(fp));
				icp=JavaCore.newLibraryEntry(cp,null,null);
				ncp[ncp.length-3]=icp;

				fp=new File(home,"lib/pcrcxcomm.jar");
				cp=new Path(FileUtilities.getAbsolutePath(fp));
				icp=JavaCore.newLibraryEntry(cp,null,null);
				ncp[ncp.length-2]=icp;				

				prj.setRawClasspath(ncp,null);
			}	
        } catch(Exception e) {
        }
		
/*		
		// TODO conditionally enable RCX menu in the main toolbar (maybe as extension point)
		URL[] pp=BootLoader.getPluginPath(LejosPlugin.plugin.getDescriptor().getInstallURL());
		for(int i=0; i<pp.length; i++) LejosPlugin.debug(pp[i]);
		
		LejosPlugin.debug("IURL: "+BootLoader.getInstallURL()+LejosPlugin.plugin.getDescriptor().getInstallURL());
*/		
	}

}