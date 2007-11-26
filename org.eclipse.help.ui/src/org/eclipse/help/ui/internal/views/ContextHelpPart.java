/*******************************************************************************
 * Copyright (c) 2000, 2007 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.help.ui.internal.views;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.runtime.Platform;
import org.eclipse.help.HelpSystem;
import org.eclipse.help.ICommandLink;
import org.eclipse.help.IContext;
import org.eclipse.help.IContext2;
import org.eclipse.help.IContext3;
import org.eclipse.help.IContextProvider;
import org.eclipse.help.IHelpResource;
import org.eclipse.help.IToc;
import org.eclipse.help.ITopic;
import org.eclipse.help.UAContentFilter;
import org.eclipse.help.internal.HelpPlugin;
import org.eclipse.help.internal.base.HelpEvaluationContext;
import org.eclipse.help.ui.internal.DefaultHelpUI;
import org.eclipse.help.ui.internal.ExecuteCommandAction;
import org.eclipse.help.ui.internal.HelpUIResources;
import org.eclipse.help.ui.internal.IHelpUIConstants;
import org.eclipse.help.ui.internal.Messages;
import org.eclipse.help.ui.internal.util.EscapeUtils;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.dialogs.IDialogPage;
import org.eclipse.jface.dialogs.IPageChangeProvider;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.jface.window.Window;
import org.eclipse.jface.wizard.IWizardContainer;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.custom.CTabFolder;
import org.eclipse.swt.custom.CTabItem;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.TabFolder;
import org.eclipse.swt.widgets.TabItem;
import org.eclipse.ui.IMemento;
import org.eclipse.ui.IViewPart;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.actions.ActionFactory;
import org.eclipse.ui.forms.IFormColors;
import org.eclipse.ui.forms.SectionPart;
import org.eclipse.ui.forms.events.ExpansionAdapter;
import org.eclipse.ui.forms.events.ExpansionEvent;
import org.eclipse.ui.forms.events.HyperlinkEvent;
import org.eclipse.ui.forms.events.IHyperlinkListener;
import org.eclipse.ui.forms.widgets.FormText;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.Section;
import org.eclipse.ui.forms.widgets.TableWrapData;
import org.eclipse.ui.forms.widgets.TableWrapLayout;

public class ContextHelpPart extends SectionPart implements IHelpPart {
	private ReusableHelpPart parent;

	private static final String HELP_KEY = "org.eclipse.ui.help"; //$NON-NLS-1$	

	private FormText text;

	private Control lastControl;

	private IContextProvider lastProvider;

	private IContext lastContext;

	private IWorkbenchPart lastPart;

	private String defaultText = ""; //$NON-NLS-1$

	private String id;
	
	private Font codeFont;

	private String savedDescription;

	/**
	 * @param parent
	 * @param toolkit
	 * @param style
	 */
	public ContextHelpPart(Composite parent, FormToolkit toolkit) {
		super(parent, toolkit, Section.EXPANDED | Section.TWISTIE
				| Section.TITLE_BAR);
		Section section = getSection();
		section.marginWidth = 5;
		section.setText(Messages.ContextHelpPart_about);
		Composite container = toolkit.createComposite(section);
		section.setClient(container);
		section.addExpansionListener(new ExpansionAdapter() {
			public void expansionStateChanged(ExpansionEvent e) {
				if (e.getState()) {
					updateText(savedDescription);
				}
			}
		});
		TableWrapLayout layout = new TableWrapLayout();
		layout.topMargin = layout.bottomMargin = 0;
		layout.leftMargin = layout.rightMargin = 0;
		layout.verticalSpacing = 10;
		container.setLayout(layout);
		text = toolkit.createFormText(container, false);
		text.setWhitespaceNormalized(false);
		text.setLayoutData(new TableWrapData(TableWrapData.FILL_GRAB));
		text.setColor(IFormColors.TITLE, toolkit.getColors().getColor(
				IFormColors.TITLE));
		codeFont = createCodeFont(parent.getDisplay(), parent.getFont(), JFaceResources.getTextFont());
		text.setFont("code", codeFont); //$NON-NLS-1$
		String key = IHelpUIConstants.IMAGE_FILE_F1TOPIC;
		text.setImage(key, HelpUIResources.getImage(key));
		key = IHelpUIConstants.IMAGE_COMMAND_F1TOPIC;
		text.setImage(key, HelpUIResources.getImage(key));
		text.addHyperlinkListener(new IHyperlinkListener() {
			public void linkActivated(HyperlinkEvent e) {
				doOpenLink(e.getHref());
			}

			public void linkEntered(HyperlinkEvent e) {
				ContextHelpPart.this.parent.handleLinkEntered(e);
			}

			public void linkExited(HyperlinkEvent e) {
				ContextHelpPart.this.parent.handleLinkExited(e);
			}
		});
		text.setText(defaultText, false, false);
	}
	
	private static Font createCodeFont(Display display, Font regularFont, Font textFont) {
		FontData[] rfontData = regularFont.getFontData();
		FontData[] tfontData = textFont.getFontData();
		int height = 0;
		
		for (int i=0; i<rfontData.length; i++) {
			FontData data = rfontData[i];
			height = Math.max(height, data.getHeight());
		}
		for (int i = 0; i < tfontData.length; i++) {
			tfontData[i].setHeight(height);
		}
		return new Font(display, tfontData);
	}
	
	public void dispose() {
		if (codeFont!=null)
			codeFont.dispose();
		codeFont = null;
		super.dispose();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.help.ui.internal.views.IHelpPart#getControl()
	 */
	public Control getControl() {
		return getSection();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.help.ui.internal.views.IHelpPart#init(org.eclipse.help.ui.internal.views.NewReusableHelpPart)
	 */
	public void init(ReusableHelpPart parent, String id, IMemento memento) {
		this.parent = parent;
		this.id = id;
		parent.hookFormText(text);
	}

	public String getId() {
		return id;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.help.ui.internal.views.IHelpPart#setVisible(boolean)
	 */
	public void setVisible(boolean visible) {
		getSection().setVisible(visible);
	}

	/**
	 * @return Returns the defaultText.
	 */
	public String getDefaultText() {
		return defaultText;
	}

	/**
	 * @param defaultText
	 *            The defaultText to set.
	 */
	public void setDefaultText(String defaultText) {
		this.defaultText = defaultText;
		if (text != null)
			text.setText(defaultText, false, false);
	}

	private void doOpenLink(Object href) {
		String sHref = (String)href;
		if (sHref.startsWith("command://")) { //$NON-NLS-1$
			doRunCommand(sHref.substring(10));
		}
		else {
			parent.showURL(sHref);
		}
	}
	
	private void doRunCommand(String serialization) {
		ExecuteCommandAction action = new ExecuteCommandAction();
		action.setInitializationString(serialization);
		action.run();
	}

	public void handleActivation(Control c, IWorkbenchPart part) {
		if (text.isDisposed())
			return;
		lastControl = c;
		lastPart = part;
		lastProvider = null;
		String helpText = createContextHelp(c);
		updateDescription(helpText);
		updateDynamicHelp(false);
	}

	private void updateDescription(String helpText) {
		if (getSection().isExpanded()) {
			updateText(helpText);
		}
		savedDescription = helpText;
	}

	private void updateDynamicHelp(boolean explicitContext) {
		if (lastContext instanceof IContext2) {
			String title = ((IContext2)lastContext).getTitle();
			if (title!=null) {
				updateDynamicHelp(stripMnemonic(title), lastControl);
				return;
			}
		}
		if (lastProvider != null || lastControl != null)
			updateDynamicHelp(lastProvider != null ? lastProvider
					.getSearchExpression(lastControl) : null, lastControl);
	}

	public void handleActivation(IContextProvider provider, IContext context, 
			Control c,
			IWorkbenchPart part) {
		if (text.isDisposed())
			return;
		if (DefaultHelpUI.isOpeningHelpView()) {
			return;
		}
		lastControl = c;
		lastProvider = provider;
		lastContext = context;
		lastPart = part;
		if (context==null && provider!=null) {
			lastContext = provider.getContext(c);
		}
		String helpText;
		if (lastContext!=null)
			helpText = formatHelpContext(lastContext);
		else
			helpText = createContextHelp(c);
		updateTitle(c);
		updateDescription(helpText);
		updateDynamicHelp(context!=null);
	}
	
	private void updateTitle(Control c) {
		String title = null;
		if (lastContext != null && lastContext instanceof IContext2) {
			IContext2 c2 = (IContext2)lastContext;
			title = c2.getTitle(); 
		}
		if (title==null && lastPart != null)
			title = NLS.bind(Messages.ContextHelpPart_aboutP, lastPart
							.getSite().getRegisteredName());
		if (title == null) {
			String[] searchTerms = computeSearchTerms(c);
			if (searchTerms.length > 0) {
				title = NLS.bind(Messages.ContextHelpPart_aboutP, (String)searchTerms[0]);
			}
		}
		if (title==null)
			title = Messages.ContextHelpPart_about;
		getSection().setText(title);
	}

	private void updateText(String helpText) {
		text.setText(helpText != null ? helpText : defaultText,
				helpText != null, 
				false);
		getSection().layout();
		getManagedForm().reflow(true);
	}

	private void updateDynamicHelp(String expression, Control c) {
		if (expression == null) {
			expression = computeDefaultSearchExpression(c);
		}
		RelatedTopicsPart part = (RelatedTopicsPart) parent
				.findPart(IHelpUIConstants.HV_RELATED_TOPICS);
		if (part != null) {
			if (expression != null) {
				if (HelpPlugin.DEBUG_CONTEXT) {
				    System.out.println("Dynamic help - search for " + expression); //$NON-NLS-1$
			    }
				part.startSearch(expression, lastContext);
			}
		}
	}

	private String computeDefaultSearchExpression(Control c) {
		String[] searchTerms = computeSearchTerms(c);
		
		StringBuffer buff = new StringBuffer();
		for (int i = 0; i < searchTerms.length; i++) {	
			if (buff.length() > 0)
				buff.append(" OR "); //$NON-NLS-1$
			buff.append('"');
			buff.append(searchTerms[i]);
			buff.append('"');
		}
		return buff.length() > 0 ? buff.toString().trim() : null;
	}

	/*
	 * Used for both dynamic help and to get a useful title
	 */
	private String[] computeSearchTerms(Control c) {
		Composite parent = c.getParent();
		List searchTerms = new ArrayList();
		while (parent != null) {
			Object data = parent.getData();
			if (data instanceof IWizardContainer) {
				IWizardContainer wc = (IWizardContainer) data;
				searchTerms.add(wc.getCurrentPage().getTitle());
				searchTerms.add(wc.getCurrentPage().getWizard().getWindowTitle());
				break;
			} else if (data instanceof IWorkbenchWindow) {
				IWorkbenchWindow window = (IWorkbenchWindow) data;
				IWorkbenchPage page = window.getActivePage();
				if (page != null) {
					IWorkbenchPart part = lastPart;
					if (part != null) {
						if (part instanceof IViewPart)
							searchTerms.add(NLS.bind(
									Messages.ContextHelpPart_query_view, part
											.getSite().getRegisteredName()));
					}
					/*
                    // Searching by perspective seems counterproductive - CG
					IPerspectiveDescriptor persp = page.getPerspective();
					if (persp != null) {
						searchTerms.add(NLS.bind(
								Messages.ContextHelpPart_query_perspective,
								persp.getLabel()));
					}
					*/
				}
				break;
			} else if (data instanceof Window) {
				Window w = (Window) data;
				if (w instanceof IPageChangeProvider) {
					Object page = ((IPageChangeProvider) w).getSelectedPage();
					String pageName = getPageName(c, page);
					if (pageName != null) {
						searchTerms.add(pageName);
					}
				}
				searchTerms.add(w.getShell().getText());
				break;
			}
			parent = parent.getParent();
		}
		return (String[]) searchTerms.toArray(new String[searchTerms.size()]);
	}

	private String getPageName(Control focusControl, Object page) {
		if (page instanceof IDialogPage)
			return ((IDialogPage) page).getTitle();
		if (focusControl == null)
			return null;

		Composite parent = focusControl.getParent();
		while (parent != null) {
			if (parent instanceof TabFolder) {
				TabItem[] selection = ((TabFolder) parent).getSelection();
				if (selection.length == 1)
					return stripMnemonic(selection[0].getText());
			} else if (parent instanceof CTabFolder) {
				CTabItem selection = ((CTabFolder) parent).getSelection();
				return stripMnemonic(selection.getText());
			}
			parent = parent.getParent();
		}
		return null;
	}
	
	private String stripMnemonic(String name) {
		int loc = name.indexOf('&');
		if (loc!= -1)
			return name.substring(0, loc)+name.substring(loc+1);
		return name;
	}

	private String createContextHelp(Control page) {
		String text = null;
		lastContext = null;
		if (page != null) {
			if (page != null /* && page.isVisible() */&& !page.isDisposed()) {
				IContext helpContext = findHelpContext(page);
				if (helpContext != null) {
					text = formatHelpContext(helpContext);
					lastContext = helpContext;
				}
			}
		}
		return text;
	}

	public static IContext findHelpContext(Control c) {
		String contextId = null;
		Control node = c;
		do {
			contextId = (String) node.getData(HELP_KEY);
			if (contextId != null)
				break;
			node = node.getParent();
		} while (node != null);
		if (contextId != null) {
			return HelpSystem.getContext(contextId);
		}
		return null;
	}

	private String formatHelpContext(IContext context) {
		String locale = Platform.getNL();
		StringBuffer sbuf = new StringBuffer();
		sbuf.append("<form>"); //$NON-NLS-1$
		sbuf.append("<p>"); //$NON-NLS-1$
		sbuf.append(decodeContextBoldTags(context));
		sbuf.append("</p>"); //$NON-NLS-1$
		
		ICommandLink[] commands = null;
		if (context instanceof IContext3) {
			commands = ((IContext3)context).getRelatedCommands();
		}
				
		String category = new String();
		if (commands != null && commands.length > 0) {
			for (int i=0;i<commands.length;++i) {
				if (!UAContentFilter.isFiltered(commands[i], HelpEvaluationContext.getContext())) {
					if (category != null) {
						addCategory(sbuf, null);
					}
					category = null;
					sbuf.append("<li style=\"image\" value=\""); //$NON-NLS-1$
					sbuf.append(IHelpUIConstants.IMAGE_COMMAND_F1TOPIC);
					sbuf.append("\" indent=\"21\">"); //$NON-NLS-1$
					sbuf.append("<a href=\"command://"); //$NON-NLS-1$
					sbuf.append(commands[i].getSerialization());
					sbuf.append("\">"); //$NON-NLS-1$	 		
					sbuf.append(EscapeUtils.escapeSpecialChars(commands[i].getLabel()));
					sbuf.append("</a>"); //$NON-NLS-1$
					sbuf.append("</li>"); //$NON-NLS-1$
				}
			}
		}
		
		IHelpResource[] links = context.getRelatedTopics();
		if (links != null && context instanceof IContext2) {
			ContextHelpSorter sorter = new ContextHelpSorter((IContext2)context);
			sorter.sort(null, links);
		}
		if (links != null && links.length > 0) {
			for (int i = 0; i < links.length; i++) {
				IHelpResource link = links[i];
				if (!UAContentFilter.isFiltered(link, HelpEvaluationContext.getContext())) {
					String cat = null;
					if (context instanceof IContext2) {
						cat = ((IContext2)context).getCategory(link);
					}
					if (cat == null && category != null || cat != null
							&& category == null || cat != null
							&& category != null && !cat.equals(category)) {
						addCategory(sbuf, cat);
					}
					category = cat;
					sbuf.append("<li style=\"image\" value=\""); //$NON-NLS-1$
					sbuf.append(IHelpUIConstants.IMAGE_FILE_F1TOPIC);
					sbuf.append("\" indent=\"21\">"); //$NON-NLS-1$
					sbuf.append("<a href=\""); //$NON-NLS-1$
					sbuf.append(link.getHref());
					String tcat = getTopicCategory(link.getHref(), locale);
					if (tcat != null && !Platform.getWS().equals(Platform.WS_GTK)) {
						sbuf.append("\" alt=\""); //$NON-NLS-1$
						sbuf.append(EscapeUtils.escapeSpecialChars(tcat));
					}
					sbuf.append("\">"); //$NON-NLS-1$	 		
					sbuf.append(EscapeUtils.escapeSpecialChars(link.getLabel()));
					sbuf.append("</a>"); //$NON-NLS-1$
					sbuf.append("</li>"); //$NON-NLS-1$
				} else {
					if (HelpPlugin.DEBUG_CONTEXT) {
						System.out.println("Link is filtered out: " + link.getLabel()); //$NON-NLS-1$
					}
				}
			}
		}
		sbuf.append("</form>"); //$NON-NLS-1$
		return sbuf.toString();
	}

	private void addCategory(StringBuffer sbuf, String category) {
		if (category == null)
			category = Messages.ContextHelpPart_seeAlso;
		sbuf.append("<p><span color=\""); //$NON-NLS-1$
		sbuf.append(IFormColors.TITLE);
		sbuf.append("\">"); //$NON-NLS-1$
		sbuf.append(category);
		sbuf.append("</span></p>"); //$NON-NLS-1$
	}

	private String getTopicCategory(String href, String locale) {
		IToc[] tocs = HelpPlugin.getTocManager().getTocs(locale);
		for (int i = 0; i < tocs.length; i++) {
			ITopic topic = tocs[i].getTopic(href);
			if (topic != null)
				return tocs[i].getLabel();
		}
		return null;
	}

	/**
	 * Make sure to support the Help system bold tag. The help system returns a
	 * regular string for getText(). Use internal apis for now to get bold.
	 * 
	 * @param context
	 * @return
	 */
	private String decodeContextBoldTags(IContext context) {
		String styledText;
		if (context instanceof IContext2) {
			styledText = ((IContext2) context).getStyledText();
			if (styledText == null) {
				styledText = context.getText();
			}
		} else {
			styledText = context.getText();
		}
		if (styledText == null) {
			return Messages.ContextHelpPart_noDescription;
		}
		String decodedString = styledText.replaceAll("<@#\\$b>", "<b>"); //$NON-NLS-1$ //$NON-NLS-2$
		decodedString = decodedString.replaceAll("</@#\\$b>", "</b>"); //$NON-NLS-1$ //$NON-NLS-2$
		decodedString = EscapeUtils.escapeSpecialCharsLeavinggBold(decodedString);
		decodedString = decodedString.replaceAll("\r\n|\n|\r", "<br/>");  //$NON-NLS-1$ //$NON-NLS-2$		
		return decodedString;
	}

	public boolean setFormInput(Object input) {
		if (input instanceof ContextHelpProviderInput) {
			ContextHelpProviderInput chinput = (ContextHelpProviderInput) input;
			//if (chinput.getContext() != null)
				handleActivation(chinput.getProvider(), chinput.getContext(), chinput.getControl(),
						chinput.getPart());
			//else
				//handleActivation(chinput.getProvider(), chinput.getControl(),
					//	chinput.getPart());
			return true;
		}
		return false;
	}

	public void setFocus() {
		if (text != null)
			text.setFocus();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.help.ui.internal.views.IHelpPart#fillContextMenu(org.eclipse.jface.action.IMenuManager)
	 */
	public boolean fillContextMenu(IMenuManager manager) {
		return parent.fillFormContextMenu(text, manager);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.help.ui.internal.views.IHelpPart#hasFocusControl(org.eclipse.swt.widgets.Control)
	 */
	public boolean hasFocusControl(Control control) {
		return text.equals(control);
	}

	public IAction getGlobalAction(String id) {
		if (id.equals(ActionFactory.COPY.getId()))
			return parent.getCopyAction();
		return null;
	}

	public void stop() {
	}

	public void toggleRoleFilter() {
	}

	public void refilter() {
	}

	public void saveState(IMemento memento) {
	}
}
