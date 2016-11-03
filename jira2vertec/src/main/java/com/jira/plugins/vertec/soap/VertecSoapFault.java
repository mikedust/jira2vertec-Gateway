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

package com.jira.plugins.vertec.soap;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import java.util.List;

public class VertecSoapFault {

    @XmlElement(name = "faultcode")
    private String code;

    @XmlElement(name = "faultstring")
    private String description;

    @XmlElement(name = "detailitem")
    @XmlElementWrapper(name = "details")
    private List<String> details;

    public String getCode() {
        return code;
    }

    public String getDescription() {
        return description;
    }

    public List<String> getDetails() {
        return details;
    }
}
