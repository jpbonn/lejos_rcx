package org.lejos.tools.eclipse.plugin.launch;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.SubProgressMonitor;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationType;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.debug.core.ILaunchManager;
import org.eclipse.debug.ui.DebugUITools;
import org.eclipse.debug.ui.IDebugModelPresentation;
import org.eclipse.debug.ui.ILaunchShortcut;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.internal.debug.ui.JDIDebugUIPlugin;
import org.eclipse.jdt.internal.debug.ui.launcher.LauncherMessages;
import org.eclipse.jdt.launching.IJavaLaunchConfigurationConstants;
import org.eclipse.jdt.ui.JavaElementLabelProvider;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.operation.IRunnableContext;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.dialogs.ElementListSelectionDialog;

/**
 * Launch shortcut.
 */
public class RunShortcut implements ILaunchShortcut
{
  /**
   * @see ILaunchShortcut#launch(IEditorPart, String)
   */
  public void launch (IEditorPart editor, String mode)
  {
    IEditorInput input = editor.getEditorInput();
    IJavaElement javaElement = (IJavaElement) input
        .getAdapter(IJavaElement.class);
    if (javaElement != null)
    {
      searchAndLaunch(new Object[]
      {javaElement}, mode);
    }
    else
    {
      MessageDialog.openInformation(getShell(), "leJOS launch",
          "no class with main method found");
    }
  }

  /**
   * @see ILaunchShortcut#launch(ISelection, String)
   */
  public void launch (ISelection selection, String mode)
  {
    if (selection instanceof IStructuredSelection)
    {
      searchAndLaunch(((IStructuredSelection) selection).toArray(), mode);
    }
  }

  /**
   * Search for classes with main method and launch one of them.
   * 
   * @param search classes to launch
   * @param mode launch mode
   */
  protected void searchAndLaunch (Object[] search, String mode)
  {
    IType type = null;

    if (search != null)
    {
      // filter / collect types
      List types = new ArrayList(search.length);
      for (int i = 0; i < search.length; i++)
      {
        if (search[i] instanceof ICompilationUnit)
        {
          try
          {
            types.addAll(Arrays.asList(((ICompilationUnit) search[i])
                .getTypes()));
          }
          catch (JavaModelException e)
          {
            // ignore
          }
        }
        if (search[i] instanceof IType)
        {
          types.add(search[i]);
        }
      }

      // launch one
      if (types.size() == 0)
      {
        MessageDialog.openInformation(getShell(), "leJOS launch",
            "no class with main method found");
      }
      else if (types.size() > 1)
      {
        type = chooseType(types, mode);
      }
      else
      {
        type = (IType) types.get(0);
      }

      if (type != null)
      {
        launch(type, mode);
      }
    }
  }

  /**
   * Prompts the user to select a type.
   * 
   * @return the selected type or <code>null</code> if none.
   */
  protected IType chooseType (List types, String mode)
  {
    ElementListSelectionDialog dialog = new ElementListSelectionDialog(
        getShell(), new JavaElementLabelProvider());
    dialog.setElements(types.toArray(new IType[types.size()]));
    dialog.setTitle(LauncherMessages.getString("leJOS launch"));
    dialog.setMessage(LauncherMessages.getString("select main class to run"));
    dialog.setMultipleSelection(false);
    if (dialog.open() == Window.OK)
    {
      return (IType) dialog.getFirstResult();
    }
    return null;
  }

  /**
   * Launches a configuration for the given type.
   */
  protected void launch (IType type, String mode)
  {
    ILaunchConfiguration config = findLaunchConfiguration(type, mode);
    if (config != null)
    {
      DebugUITools.launch(config, mode);
    }
  }

  /**
   * Locate a configuration to relaunch for the given type. If one cannot be
   * found, create one.
   * 
   * @return a re-useable config or <code>null</code> if none
   */
  protected ILaunchConfiguration findLaunchConfiguration (IType type,
      String mode)
  {
    ILaunchConfigurationType configType = getLejosLaunchConfigType();
    List candidateConfigs = Collections.EMPTY_LIST;
    try
    {
      ILaunchConfiguration[] configs = DebugPlugin.getDefault()
          .getLaunchManager().getLaunchConfigurations(configType);
      candidateConfigs = new ArrayList(configs.length);
      for (int i = 0; i < configs.length; i++)
      {
        ILaunchConfiguration config = configs[i];
        if (config.getAttribute(
            IJavaLaunchConfigurationConstants.ATTR_MAIN_TYPE_NAME, "").equals(
            type.getFullyQualifiedName()))
        {
          if (config.getAttribute(
              IJavaLaunchConfigurationConstants.ATTR_PROJECT_NAME, "").equals(
              type.getJavaProject().getElementName()))
          {
            candidateConfigs.add(config);
          }
        }
      }
    }
    catch (CoreException e)
    {
      JDIDebugUIPlugin.log(e);
    }

    // If there are no existing configs associated with the IType, create one.
    // If there is exactly one config associated with the IType, return it.
    // Otherwise, if there is more than one config associated with the IType,
    // prompt the
    // user to choose one.
    int candidateCount = candidateConfigs.size();
    if (candidateCount < 1)
    {
      return createConfiguration(type);
    }
    else if (candidateCount == 1)
    {
      return (ILaunchConfiguration) candidateConfigs.get(0);
    }
    else
    {
      // Prompt the user to choose a config. A null result means the user
      // cancelled the dialog, in which case this method returns null,
      // since cancelling the dialog should also cancel launching anything.
      ILaunchConfiguration config = chooseConfiguration(candidateConfigs, mode);
      if (config != null)
      {
        return config;
      }
    }
    return null;
  }

  /**
   * Create & return a new configuration based on the specified
   * <code>IType</code>.
   */
  protected ILaunchConfiguration createConfiguration (IType type)
  {
    ILaunchConfiguration config = null;
    try
    {
      ILaunchConfigurationType configType = getLejosLaunchConfigType();
      ILaunchConfigurationWorkingCopy wc = configType
          .newInstance(null, DebugPlugin.getDefault().getLaunchManager()
              .generateUniqueLaunchConfigurationNameFrom(type.getElementName()));
      wc.setAttribute(IJavaLaunchConfigurationConstants.ATTR_MAIN_TYPE_NAME,
          type.getFullyQualifiedName());
      wc.setAttribute(IJavaLaunchConfigurationConstants.ATTR_PROJECT_NAME, type
          .getJavaProject().getElementName());
      config = wc.doSave();
    }
    catch (CoreException ce)
    {
      JDIDebugUIPlugin.log(ce);
    }
    return config;
  }

  /**
   * Show a selection dialog that allows the user to choose one of the specified
   * launch configurations. Return the chosen config, or <code>null</code> if
   * the user cancelled the dialog.
   */
  protected ILaunchConfiguration chooseConfiguration (List configList,
      String mode)
  {
    IDebugModelPresentation labelProvider = DebugUITools
        .newDebugModelPresentation();
    ElementListSelectionDialog dialog = new ElementListSelectionDialog(
        getShell(), labelProvider);
    dialog.setElements(configList.toArray());
    dialog.setTitle(LauncherMessages.getString("leJOS launch"));
    dialog.setMessage(LauncherMessages.getString("select configuration for running main class"));
    dialog.setMultipleSelection(false);
    int result = dialog.open();
    labelProvider.dispose();
    if (result == Window.OK)
    {
      return (ILaunchConfiguration) dialog.getFirstResult();
    }
    return null;
  }

  /**
   * Returns the local java launch config type
   */
  protected ILaunchConfigurationType getLejosLaunchConfigType ()
  {
    // TODO introduce constant
    ILaunchManager lm = DebugPlugin.getDefault().getLaunchManager();
    return lm.getLaunchConfigurationType("org.lejos.ldt.core.run");
  }

  /**
   * Convenience method to get the window that owns this action's Shell.
   */
  protected Shell getShell ()
  {
    return JDIDebugUIPlugin.getActiveWorkbenchShell();
  }

  public IType[] findMains (IRunnableContext context, final IType[] elements)
      throws InvocationTargetException, InterruptedException
  {
    final Set result = new HashSet();

    if (elements.length > 0)
    {
      IRunnableWithProgress runnable = new IRunnableWithProgress()
      {
        public void run (IProgressMonitor pm) throws InterruptedException
        {
          int nElements = elements.length;
          pm.beginTask("searching classes with main methods", nElements);
          try
          {
            for (int i = 0; i < nElements; i++)
            {
              if (hasMain(elements[i]))
              {
                result.add(elements[i]);
              }
              pm.worked(1);
              if (pm.isCanceled())
              {
                throw new InterruptedException();
              }
            }
          }
          finally
          {
            pm.done();
          }
        }
      };
      context.run(true, true, runnable);
    }
    return (IType[]) result.toArray(new IType[result.size()]);
  }

  /**
   * Check if type has main method.
   * 
   * @param type type to check
   */
  protected boolean hasMain (IType type)
  {
    try
    {
      IMethod method = type.getMethod("main", new String[]
      {"[QString;"});
      return method != null && method.isMainMethod();
    }
    catch (JavaModelException e)
    {
      return false;
    }
  }
}