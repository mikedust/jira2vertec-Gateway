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

package com.jira.plugins.vertec;

import com.jira.plugins.configuration.JiraToVertecConfiguration;
import com.jira.plugins.vertec.entities.*;
import com.jira.plugins.vertec.service.VertecConnector;
import com.jira.plugins.vertec.service.VertecSerializer;
import com.jira.plugins.vertec.service.VertecServiceException;
import com.jira.plugins.vertec.soap.*;
import org.apache.log4j.Logger;

import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import java.io.*;
import java.util.Collections;
import java.util.Date;
import java.util.List;

public class VertecRepositoryImpl implements VertecRepository
{
    private VertecConnector connector;
    private VertecSerializer serializer;
    private JiraToVertecConfiguration configuration;

    private final static Logger logger = Logger.getLogger(VertecRepositoryImpl.class);

    public VertecRepositoryImpl(JiraToVertecConfiguration configuration, VertecConnector connector, VertecSerializer serializer)
    {
        this.configuration = configuration;
        this.connector = connector;
        this.serializer = serializer;
    }

    public List<VertecProject> getAllProjectsAktiv()
    {
        VertecSoapSelection selection = new VertecSoapSelection();
        selection.setOcl("projekt->select(aktiv)");
        VertecSoapEnvelope e = getEnvelope(selection, VertecProject.createResultDef(configuration.zeigeProjektbeschrieb()));

        List<VertecProject> projekte = executeListQuery(e);

        Collections.sort(projekte);
        return projekte;
    }

    public VertecProject getProjekt(String phaseId)
    {
        VertecSoapSelection selection = new VertecSoapSelection();
        selection.setOcl(String.format("projektphase->select((boldid=%s) and aktiv)->first.projekt", phaseId));
        VertecSoapEnvelope e = getEnvelope(selection, VertecProject.createResultDef(configuration.zeigeProjektbeschrieb()));

        List<VertecProject> projekte = executeListQuery(e);

        if (projekte.size() > 1) {
            logger.warn(String.format("[JiraToVertec] Mehr als ein Projekt fuer die Phasen-ID '%s' erhalten!", phaseId));
        } else if (projekte.size() == 0) {
            logger.warn(String.format("[JiraToVertec] Kein Projekt fuer die Phasen-ID '%s' gefunden!", phaseId));
            return null;
        }
        return projekte.get(0);
    }

    public List<VertecPhase> getProjektPhasenAktiv(String projektId) {
        return getProjektPhasenAktiv(projektId, false);
    }

    public List<VertecPhase> getProjektPhasenAktivMitProjektbeschrieb(String projektId) {
        return getProjektPhasenAktiv(projektId, true);
    }
    
    private List<VertecPhase> getProjektPhasenAktiv(String projektId, boolean zeigeProjektbeschrieb)
    {
        VertecSoapSelection selection = new VertecSoapSelection();
        selection.setObjRef(projektId);
        selection.setOcl("phasen->select(aktiv)");
        VertecSoapEnvelope e = getEnvelope(selection, VertecPhase.createResultDef(zeigeProjektbeschrieb));

        List<VertecPhase> phasen = executeListQuery(e);
        Collections.sort(phasen);
        return phasen;
    }

    public VertecPhase getPhase(String phaseId) {
        return getPhase(phaseId, false);
    }

    public VertecPhase getPhaseMitProjektbeschrieb(String phaseId) {
        return getPhase(phaseId, true);
    }

    private VertecPhase getPhase(String phaseId, boolean zeigeProjektbeschrieb)
    {
        if (phaseId == null || phaseId.length() == 0)
            return null;
        
        VertecSoapSelection selection = new VertecSoapSelection();
        selection.setObjRef(phaseId);
        VertecSoapEnvelope e = getEnvelope(selection, VertecPhase.createResultDef(zeigeProjektbeschrieb));

        List<VertecPhase> phasen = executeListQuery(e);
        
        if (phasen.size() > 1)
        {
            logger.warn(String.format("[JiraToVertec] Mehr als eine Phase fuer die ID '%s' erhalten!", phaseId));
        }
        else if (phasen.size() == 0)
        {
            logger.warn(String.format("[JiraToVertec] Keine Phase fuer die ID '%s' gefunden!", phaseId));
            return null;
        }

        logger.info(String.format("[JiraToVertec] Phase mit der Id '%s' aus Vertec ausgelesen.", phaseId));

        return phasen.get(0);
    }

    public VertecProjektBearbeiter getBenutzer(String loginName)
    {
        VertecSoapSelection selection = new VertecSoapSelection();
        selection.setOcl("ProjektBearbeiter");
        selection.setSqlWhere(String.format("loginName='%1s'", loginName));
        VertecSoapEnvelope e = getEnvelope(selection, VertecProjektBearbeiter.createResultDef());

        List<VertecProjektBearbeiter> benutzer = executeListQuery(e);
        if (benutzer.size() > 1)
        {
            logger.error(String.format("[JiraToVertec] Mehr als einen ProjektBearbeiter fuer das Login '%s' gefunden!", loginName));
            return null;
        }
        else if (benutzer.size() == 0)
        {
            logger.error(String.format("[JiraToVertec] Keinen ProjektBearbeiter fuer das Login '%s' gefunden!", loginName));
            return null;
        }
        else if (!benutzer.get(0).isActive())
        {
            logger.error(String.format("[JiraToVertec] Der ProjektBearbeiter mit dem Login '%s' ist nicht aktiv!", loginName));
            return null;
        }
        logger.info(String.format("[JiraToVertec] ProjektBearbeiter mit dem Login '%s' aus Vertec ausgelesen.", loginName));
        return benutzer.get(0);
    }


    public void updateLeistung(String leistungId, int minuten, Date date, String vertecPhaseId, String comment)
    {
        VertecOffeneLeistung leistung = new VertecOffeneLeistung();
        leistung.setObjref(leistungId);
        leistung.setDatum(date);
        leistung.setPhaseId(vertecPhaseId);
        leistung.setMinuten(minuten);
        leistung.setText(comment);

        VertecSoapUpdate<VertecOffeneLeistung> update = new VertecSoapUpdate<VertecOffeneLeistung>();
        update.setItem(leistung);

        VertecSoapEnvelope e = new VertecSoapEnvelope(configuration.getVertecServiceUser(), configuration.getVertecServicePassword());
        e.getBody().setContent(update);

        executeQuery(e);

        logger.info(String.format("[JiraToVertec] Leistung wurde aktualisiert: Leistung:%s, Phase:%s, Minuten:%d, Datum:%s, Kommentar:'%s')", leistungId, vertecPhaseId, minuten, date, comment));
    }

    public void createLeistung(int minuten, String jiraReferenz, String benutzerId, String vertecPhaseId, Date date, String comment) throws VertecServiceException
    {
        VertecOffeneLeistung leistung = new VertecOffeneLeistung();
        leistung.setDatum(date);
        leistung.setPhaseId(vertecPhaseId);
        leistung.setBearbeiterId(benutzerId);
        leistung.setJiraReferenz(jiraReferenz);
        leistung.setMinuten(minuten);
        leistung.setText(comment);

        VertecSoapCreate<VertecOffeneLeistung> create = new VertecSoapCreate<VertecOffeneLeistung>();
        create.setItem(leistung);

        VertecSoapEnvelope e = new VertecSoapEnvelope(configuration.getVertecServiceUser(), configuration.getVertecServicePassword());
        e.getBody().setContent(create);

        VertecSoapCreateResponse<VertecOffeneLeistung> response = executeQuery(e);

        if (response == null || response.getItem() == null || !response.getItem().isValid())
        {
            throw new VertecServiceException("[JiraToVertec] Die neu erzeugte Leistung wurde von Vertec nicht akzeptiert!\n  (Phase:%s, Minuten:%d, User:%s, Datum:%s, JiraId:%s)", vertecPhaseId, minuten, benutzerId, date, jiraReferenz);
        }

        logger.info(String.format("[JiraToVertec] Leistung wurde erstellt: Phase:%s, Minuten:%d, Benutzer:%s, Datum:%s, Kommentar:'%s', JiraReferenz:%s)", vertecPhaseId, minuten, benutzerId, date, comment, jiraReferenz));
    }

    public void deleteLeistung(String leistungId) throws VertecServiceException
    {
        VertecSoapDelete delete = new VertecSoapDelete();
        delete.setObjref(leistungId);

        VertecSoapEnvelope e = new VertecSoapEnvelope(configuration.getVertecServiceUser(), configuration.getVertecServicePassword());
        e.getBody().setContent(delete);

        VertecSoapDeleteResponse response = executeQuery(e);

        if (response == null || !"Deleted 1 Objects".equalsIgnoreCase(response.getText()))
        {
            throw new VertecServiceException("[JiraToVertec] Die Leistung konnte in vertec nicht geloescht werden!\n  (Id:%s)", leistungId);
        }

        logger.info(String.format("[JiraToVertec] Leistung wurde geloescht: %s", leistungId));
    }

    public List<VertecLeistung> getLeistungen(String jiraReferenz)
    {
        List<VertecLeistung> leistungen = getVertecSoapEnvelopeForReferenzByOcl("Leistung", jiraReferenz);
        
        if (logger.isInfoEnabled())
            logger.info(String.format("[JiraToVertec] %s Leistung(en) mit der Jira Worklog-ID %s wurden aus Vertec ausgelesen.", leistungen == null ? 0 : leistungen.size(), jiraReferenz));

        return leistungen;
    }

    public List<VertecOffeneLeistung> getOffeneLeistungen(String jiraReferenz)
    {        
        List<VertecOffeneLeistung> leistungen = getVertecSoapEnvelopeForReferenzByOcl("OffeneLeistung", jiraReferenz);

        if (logger.isInfoEnabled())
            logger.info(String.format("[JiraToVertec] %s offene Leistung(en) mit der Jira Worklog-ID %s wurden aus Vertec ausgelesen.", leistungen == null ? 0 : leistungen.size(), jiraReferenz));

        return leistungen;
    }

    private <T> List<T>  getVertecSoapEnvelopeForReferenzByOcl(String ocl, String jiraReferenz) {
        VertecSoapSelection selection = new VertecSoapSelection();
        selection.setOcl(ocl);
        selection.setSqlWhere(String.format("referenz LIKE '%1s'", jiraReferenz));
        VertecSoapEnvelope vertecSoapEnvelope = getEnvelope(selection, VertecLeistung.createResultDef());

        return executeListQuery(vertecSoapEnvelope);
    }

    public VertecDateTimeProperty getSperrdatum()
    {
        VertecSoapSelection selection = new VertecSoapSelection();
        selection.setOcl("datetimeproperty->select(propertyName='Sperrdatum')->first");
        VertecSoapEnvelope e = getEnvelope(selection, VertecDateTimeProperty.createResultDef());

        List<VertecDateTimeProperty> datum = executeListQuery(e);
        if (datum.size() == 0) {
            logger.warn("[JiraToVertec] Das Sperrdatum konnte nicht ausgelesen werden!");
            return null;
        }

        if (logger.isInfoEnabled())
            logger.info(String.format("[JiraToVertec] Sperrdatum (%s) wurde aus Vertec ausgelesen.", datum.get(0).getPropertyValue()));
        
        return datum.get(0);
    }

    public VertecFreigabe getPersFreigabedatum(String user)
    {
        if (user == null || user.length() == 0) {
            logger.warn("[JiraToVertec] Das persoenliche Freigabedatum konnte nicht ausgelesen werden, da im Worklog kein Benutzer aufgefuehrt ist!");
            return null;
        }

        VertecSoapSelection selection = new VertecSoapSelection();
        selection.setOcl(String.format("projektbearbeiter->select(loginName='%s').eigenefreigaben->select(freigabeselbst)->orderdescending(bis)->first", user));
        VertecSoapEnvelope e = getEnvelope(selection, VertecFreigabe.createResultDef());

        List<VertecFreigabe> datum = executeListQuery(e);
        if (datum.size() == 0) {
            logger.warn("[JiraToVertec] Das persoenliche Freigabedatum konnte nicht ausgelesen werden!");
            return null;
        }

        if (logger.isInfoEnabled())
            logger.info(String.format("[JiraToVertec] Das persoenliche Freigabedatum (%s) wurde aus Vertec ausgelesen.", datum.get(0).getBis()));

        return datum.get(0);
    }

    private  <T> List<T> executeListQuery(VertecSoapEnvelope envelope) {
        try
        {
            String xmlQuery = serializer.SerializeObject(envelope);
            logger.debug(String.format("[JiraToVertec] Anfrage an Vertec: %s", xmlQuery));
            InputStream responseStream = connector.Query(xmlQuery);

            return serializer.DeserializeResponseList(responseStream);
        }
        catch (JAXBException e1)
        {
            logger.error("[JiraToVertec] JABException beim Laden von Daten aus Vertec.", e1);
        }
        catch (IOException e1)
        {
            logger.error("[JiraToVertec] IOException beim Laden von Daten aus Vertec.", e1);
        }
        catch (VertecServiceException e1)
        {
            logger.warn(String.format("[JiraToVertec] Vertec Fault beim Laden von Daten aus Vertec: '\n%s'", e1.getMessage()));
        }
        return Collections.emptyList();
    }

    private <T> T executeQuery(VertecSoapEnvelope envelope)
    {
        try
        {
            String xmlQuery = serializer.SerializeObject(envelope);
            logger.debug(String.format("[JiraToVertec] Anfrage an Vertec: %s", xmlQuery));
            InputStream responseStream = connector.Query(xmlQuery);
            //String s = convert(responseStream);
            return serializer.DeserializeResponse(responseStream);
        }
        catch (JAXBException e1)
        {
            logger.error("[JiraToVertec] JABException beim Laden von Daten aus Vertec.", e1);
        }
        catch (IOException e1)
        {
            logger.error("[JiraToVertec] IOException beim Laden von Daten aus Vertec.", e1);
        }
        catch (VertecServiceException e1)
        {
            logger.warn(String.format("[JiraToVertec] Vertec Fault beim Laden von Daten aus Vertec: '\n%s'", e1.getMessage()));
        }
        return null;
    }


    private VertecSoapEnvelope getEnvelope(VertecSoapSelection selection, VertecSoapResultDef resultDef)
    {
        VertecSoapEnvelope e = new VertecSoapEnvelope(configuration.getVertecServiceUser(), configuration.getVertecServicePassword());
        VertecSoapQuery query = new VertecSoapQuery();
        e.getBody().setContent(query);
        query.setResultDef(resultDef);
        query.setSelection(selection);
        return e;
    }

}
