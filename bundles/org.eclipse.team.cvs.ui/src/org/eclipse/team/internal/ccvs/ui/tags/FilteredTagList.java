/*******************************************************************************
 * Copyright (c) 2000, 2006 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.team.internal.ccvs.ui.tags;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.team.internal.ccvs.core.CVSTag;
import org.eclipse.team.internal.ccvs.core.util.StringMatcher;
import org.eclipse.ui.model.IWorkbenchAdapter;

/**
 * Workbench model element that returns a filtered list of tags
 */
public class FilteredTagList implements IWorkbenchAdapter, IAdaptable {

    private final TagSource tagSource;
    private final int[] types;
    private StringMatcher matcher;

    public FilteredTagList(TagSource tagSource, int[] types) {
        this.tagSource = tagSource;
        this.types = types;
    }
    
    /* (non-Javadoc)
     * @see org.eclipse.ui.model.IWorkbenchAdapter#getChildren(java.lang.Object)
     */
    public Object[] getChildren(Object o) {
        CVSTag[] tags = getTags();
        List filtered = new ArrayList();
        for (int i = 0; i < tags.length; i++) {
            CVSTag tag = tags[i];
            if (select(tag)) {
                filtered.add(new TagElement(this, tag));
            }
        }
        return filtered.toArray(new Object[filtered.size()]);
    }

    private boolean select(CVSTag tag) {
        if (matcher == null) return true;
        return matcher.match(tag.getName());
    }

    private CVSTag[] getTags() {
        return tagSource.getTags(types);
    }

    /* (non-Javadoc)
     * @see org.eclipse.ui.model.IWorkbenchAdapter#getImageDescriptor(java.lang.Object)
     */
    public ImageDescriptor getImageDescriptor(Object object) {
        return null;
    }

    /* (non-Javadoc)
     * @see org.eclipse.ui.model.IWorkbenchAdapter#getLabel(java.lang.Object)
     */
    public String getLabel(Object o) {
        return null;
    }

    /* (non-Javadoc)
     * @see org.eclipse.ui.model.IWorkbenchAdapter#getParent(java.lang.Object)
     */
    public Object getParent(Object o) {
        return null;
    }

    /* (non-Javadoc)
     * @see org.eclipse.core.runtime.IAdaptable#getAdapter(java.lang.Class)
     */
    public Object getAdapter(Class adapter) {
		if (adapter == IWorkbenchAdapter.class) return this;
		return null;
    }
    
    public void setPattern(String pattern) {
        if (!pattern.endsWith("*")) { //$NON-NLS-1$
            pattern += "*"; //$NON-NLS-1$
        }
        matcher = new StringMatcher(pattern, true, false);
    }

    public CVSTag[] getMatchingTags() {
        CVSTag[] tags = getTags();
        List filtered = new ArrayList();
        for (int i = 0; i < tags.length; i++) {
            CVSTag tag = tags[i];
            if (select(tag)) {
                filtered.add(tag);
            }
        }
        return (CVSTag[])filtered.toArray(new CVSTag[filtered.size()]);
    }

}
