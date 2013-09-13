/*
 * Copyright (c) 2007-2013 The Broad Institute, Inc.
 * SOFTWARE COPYRIGHT NOTICE
 * This software and its documentation are the copyright of the Broad Institute, Inc. All rights are reserved.
 *
 * This software is supplied without any warranty or guaranteed support whatsoever. The Broad Institute is not responsible for its use, misuse, or functionality.
 *
 * This software is licensed under the terms of the GNU Lesser General Public License (LGPL),
 * Version 2.1 which is available at http://www.opensource.org/licenses/lgpl-2.1.php.
 */

package org.broad.igv.util.collections;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;

/**
 * List which keeps track of statistics on members.
 * Currently the only thing it keeps track of is the
 * maximum.
 *
 * NOTE: Does not support object removal, an {@code UnsupportedOperationException}
 * should be thrown in that case.
 * @author jacob
 * @date 2013-Sep-13
 */
public class StatList<T> extends ArrayList<T>{

    private Comparator<T> comparator;
    private T max;

    public StatList(int elements, Comparator<T> comparator){
        super(elements);
        this.comparator = comparator;
    }

    @Override
    public boolean add(T el){
        updateStats(el);
        return super.add(el);
    }

    @Override
    public void add(int index, T el){
        updateStats(el);
        super.add(index, el);
    }

    @Override
    public T remove(int index) {
        throw new UnsupportedOperationException("Cannot remove from StatList");
    }

    @Override
    public boolean remove(Object el) {
        throw new UnsupportedOperationException("Cannot remove from StatList");
    }

    @Override
    public boolean addAll(Collection<? extends T> els){
        for(T el: els){
            updateStats(el);
        }
        return super.addAll(els);
    }

    private void updateStats(T el){
        if(max == null || this.comparator.compare(el, max) > 0){
            this.max = el;
        }
    }

    public T getMax() {
        return max;
    }

}
