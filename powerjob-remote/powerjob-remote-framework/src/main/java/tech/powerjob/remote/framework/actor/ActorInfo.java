package tech.powerjob.remote.framework.actor;


/**
 * ActorInfo
 *
 * @author tjq
 * @since 2022/12/31
 */
public class ActorInfo {

    private final Object actor;

    private final Actor anno;

    public ActorInfo(Object actor, Actor anno) {
        this.actor = actor;
        this.anno = anno;
    }

    public Object getActor() {
        return actor;
    }

    public Actor getAnno() {
        return anno;
    }
}
