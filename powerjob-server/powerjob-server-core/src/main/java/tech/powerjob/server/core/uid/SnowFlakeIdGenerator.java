package tech.powerjob.server.core.uid;

/**
 * Twitter SnowFlake（Scala -> Java）
 *
 * @author tjq
 * @since 2020/4/6
 */
class SnowFlakeIdGenerator {

    /**
     * 起始的时间戳(a special day for me)
     */
    private final static long START_STAMP = 1555776000000L;

    /**
     * 每一部分占用的位数
     */
    private final static long SEQUENCE_BIT = 6; //序列号占用的位数
    private final static long MACHINE_BIT = 14;   //机器标识占用的位数
    private final static long DATA_CENTER_BIT = 2;//数据中心占用的位数

    /**
     * 每一部分的最大值
     */
    private final static long MAX_DATA_CENTER_NUM = ~(-1L << DATA_CENTER_BIT);
    private final static long MAX_MACHINE_NUM = ~(-1L << MACHINE_BIT);
    private final static long MAX_SEQUENCE = ~(-1L << SEQUENCE_BIT);

    /**
     * 每一部分向左的位移
     */
    private final static long MACHINE_LEFT = SEQUENCE_BIT;
    private final static long DATA_CENTER_LEFT = SEQUENCE_BIT + MACHINE_BIT;
    private final static long TIMESTAMP_LEFT = DATA_CENTER_LEFT + DATA_CENTER_BIT;

    private final long dataCenterId;  //数据中心
    private final long machineId;     //机器标识
    private long sequence = 0L; //序列号
    private long lastTimestamp = -1L;//上一次时间戳

    public SnowFlakeIdGenerator(long dataCenterId, long machineId) {
        if (dataCenterId > MAX_DATA_CENTER_NUM || dataCenterId < 0) {
            throw new IllegalArgumentException("dataCenterId can't be greater than MAX_DATA_CENTER_NUM or less than 0");
        }
        if (machineId > MAX_MACHINE_NUM || machineId < 0) {
            throw new IllegalArgumentException("machineId can't be greater than MAX_MACHINE_NUM or less than 0");
        }
        this.dataCenterId = dataCenterId;
        this.machineId = machineId;
    }

    /**
     * 产生下一个ID
     */
    public synchronized long nextId() {
        long currStamp = getNewStamp();
        if (currStamp < lastTimestamp) {
            throw new RuntimeException("clock moved backwards, refusing to generate id");
        }

        if (currStamp == lastTimestamp) {
            //相同毫秒内，序列号自增
            sequence = (sequence + 1) & MAX_SEQUENCE;
            //同一毫秒的序列数已经达到最大
            if (sequence == 0L) {
                currStamp = getNextMill();
            }
        } else {
            //不同毫秒内，序列号置为0
            sequence = 0L;
        }

        lastTimestamp = currStamp;

        return (currStamp - START_STAMP) << TIMESTAMP_LEFT //时间戳部分
                | dataCenterId << DATA_CENTER_LEFT       //数据中心部分
                | machineId << MACHINE_LEFT             //机器标识部分
                | sequence;                             //序列号部分
    }

    private long getNextMill() {
        long mill = getNewStamp();
        while (mill <= lastTimestamp) {
            mill = getNewStamp();
        }
        return mill;
    }

    private long getNewStamp() {
        return System.currentTimeMillis();
    }
}
