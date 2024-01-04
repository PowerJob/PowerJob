package tech.powerjob.server.persistence.storage.impl;

import tech.powerjob.common.utils.NetUtils;
import tech.powerjob.server.extension.dfs.DFsService;

import java.util.Optional;

/**
 * MySqlSeriesDfsServiceTest
 *
 * @author tjq
 * @since 2023/8/10
 */
class MySqlSeriesDfsServiceTest extends AbstractDfsServiceTest {

    @Override
    protected Optional<DFsService> fetchService() {

        boolean dbAvailable = NetUtils.checkIpPortAvailable("127.0.0.1", 3306);
        if (dbAvailable) {
            MySqlSeriesDfsService mySqlSeriesDfsService = new MySqlSeriesDfsService();

            try {

                MySqlSeriesDfsService.MySQLProperty mySQLProperty = new MySqlSeriesDfsService.MySQLProperty()
                        .setDriver("com.mysql.cj.jdbc.Driver")
                        .setUrl("jdbc:mysql://localhost:3306/powerjob-daily?useUnicode=true&characterEncoding=UTF-8&serverTimezone=Asia/Shanghai")
                        .setUsername("root")
                        .setAutoCreateTable(true)
                        .setPassword("No1Bug2Please3!");
                mySqlSeriesDfsService.initDatabase(mySQLProperty);
                mySqlSeriesDfsService.initTable(mySQLProperty);

                return Optional.of(mySqlSeriesDfsService);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        return Optional.empty();
    }
}