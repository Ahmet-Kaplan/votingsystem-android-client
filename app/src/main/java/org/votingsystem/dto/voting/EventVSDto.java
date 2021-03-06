package org.votingsystem.dto.voting;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import org.votingsystem.dto.CommentVSDto;
import org.votingsystem.util.TypeVS;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;

/**
 * Licence: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class EventVSDto implements Serializable {

    private static final long serialVersionUID = 1L;

    public static final String TAG = EventVSDto.class.getSimpleName();

    public enum State {ACTIVE, TERMINATED, CANCELLED, PENDING, PENDING_SIGNATURE, DELETED_FROM_SYSTEM}

    public enum Type { CLAIM, MANIFEST, ELECTION }

    public enum Cardinality { MULTIPLE, EXCLUSIVE}

    private Long id;
    private Long eventId;
    private TypeVS typeVS = TypeVS.VOTING_EVENT;
    private Cardinality cardinality;
    private String content;
    private String subject;
    private String URL;
    private Integer numSignaturesCollected;
    private Integer numVotesCollected;
    private ControlCenterDto controlCenter;
    private String userVS;
    private AccessControlDto accessControl;
    private Integer numComments = 0;

    private Set<FieldEventVSDto> fieldEventVSSet = new HashSet<FieldEventVSDto>();
    private Set<EventTagVS> eventTagVSSet = new HashSet<EventTagVS>();
    private Set<CommentVSDto> commentVSDtoSet = new HashSet<CommentVSDto>();

    private Date dateBegin;
    private Date dateFinish;
    private Date dateCreated;
    private Date lastUpdated;
    private String[] tags;
    private State state;
    private VoteVSDto vote;
    private String UUID;

    public EventVSDto() {}

    public TypeVS getTypeVS() {
        return typeVS;
    }

    public void setTypeVS(TypeVS typeVS) {
        this.typeVS = typeVS;
    }

    public String getContent () {
        return content;
    }

    public void setContent (String content) {
        this.content = content;
    }

    public String getSubject() { return subject; }

    public void setSubject (String subject) {
        this.subject = subject;
    }

    public Cardinality getElectionType() {
        return cardinality;
    }

    public Date getDateCreated() {
        return dateCreated;
    }

    public void setDateCreated(Date dateCreated) {
        this.dateCreated = dateCreated;
    }

    public Date getLastUpdated() {
        return lastUpdated;
    }

    public void setLastUpdated(Date lastUpdated) {
        this.lastUpdated = lastUpdated;
    }

    public void setCardinality(Cardinality cardinality) {
        this.cardinality = cardinality;
    }

    public Set<FieldEventVSDto> getFieldsEventVS() {
        return fieldEventVSSet;
    }

    public void setFieldsEventVS(Set<FieldEventVSDto> fieldsEventVS) {
        this.fieldEventVSSet = fieldsEventVS;
    }

    public void setEventTagVSes(Set<EventTagVS> eventTagVSSet) {
        this.eventTagVSSet = eventTagVSSet;
    }

    public Set<EventTagVS> getEventTagVSes() {
        return eventTagVSSet;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getId() {
        return id;
    }

    public void setEventVSId(Long eventId) {
        this.eventId = eventId;
    }

    public Long getEventVSId() {
        return eventId;
    }

    public String[] getTags() {
        return tags;
    }

    public String getUserVS() {
        return userVS;
    }

    public void setUserVS(String userVS) {
        this.userVS = userVS;
    }

    public void setTags(String[] tags) {
        if (tags.length == 0) return;
        ArrayList<String> arrayTags = new ArrayList<String>();
        for (String tag:tags) {
            arrayTags.add(tag.toLowerCase());
        }
        this.tags = arrayTags.toArray(tags);
    }

    public void setCommentVSes(Set<CommentVSDto> commentVSDtoSet) {
        this.commentVSDtoSet = commentVSDtoSet;
    }

    public Set<CommentVSDto> getCommentVSes() {
        return commentVSDtoSet;
    }

    public void setNumComments(int numComments) {
        this.numComments = numComments;
    }

    public int getNumComments() {
        return numComments;
    }

    public ControlCenterDto getControlCenter() {
        return controlCenter;
    }

    public void setControlCenter(ControlCenterDto controlCenter) {
        this.controlCenter = controlCenter;
    }

    public Integer getNumSignaturesCollected() {
        return numSignaturesCollected;
    }

    public void setNumSignaturesCollected(Integer numSignaturesCollected) {
        this.numSignaturesCollected = numSignaturesCollected;
    }

    public Integer getNumVotesCollected() {
        return numVotesCollected;
    }

    public void setNumVotesCollected(Integer numVotesCollected) {
        this.numVotesCollected = numVotesCollected;
    }

    public Date getDateBegin() {
        return dateBegin;
    }

    public void setDateBegin(Date dateBegin) {
        this.dateBegin = dateBegin;
    }

    public Date getDateFinish() {
        return dateFinish;
    }

    public void setDateFinish(Date dateFinish) {
        this.dateFinish = dateFinish;
    }

    public AccessControlDto getAccessControl() {
        return accessControl;
    }

    public void setAccessControlVS(AccessControlDto accessControl) {
        this.accessControl = accessControl;
    }


    public void setVote(VoteVSDto vote) {
        this.vote = vote;
    }

    public VoteVSDto getVote() {
        return vote;
    }

    public String getStatsServiceURL() {
        return accessControl.getServerURL() + "/rest/eventVSElection/id/" + id + "/stats";
    }

    public boolean isActive() {
        Date todayDate = java.util.Calendar.getInstance().getTime();
        if (todayDate.after(dateBegin) && todayDate.before(dateFinish)) return true;
        else return false;
    }

    public void setState(State state) {
        this.state = state;
    }

    public State getState() {
        return this.state;
    }

    public String getURL() {
        return URL;
    }

    public void setURL(String uRL) {
        URL = uRL;
    }

    public String getUUID() {
        return UUID;
    }

    public void setUUID(String UUID) {
        this.UUID = UUID;
    }

}