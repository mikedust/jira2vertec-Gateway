/**
 * Copyright 2013 GARAIO AG <www.garaio.com>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.jira.plugins.vertec.entities;

import com.jira.plugins.util.BooleanAdapter;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;


public class VertecBaseEntity
{
    private Boolean isValid;

    private String id;

    @XmlElement(name = "objref")
    public String getObjref()
    {
        return objref;
    }

    public void setObjref(String objref)
    {
        this.objref = objref;
    }

    private String objref;

    @XmlElement(name = "isValid")
    @XmlJavaTypeAdapter(BooleanAdapter.class)
    public Boolean isValid()
    {
        return isValid;
    }

    public void setValid(Boolean valid)
    {
        isValid = valid;
    }

    @XmlElement(name = "objid")
    public String getId()
    {
        return id;
    }

    public void setId(String id)
    {
        this.id = id;
    }
}
