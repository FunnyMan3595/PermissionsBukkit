package com.platymuus.bukkit.permissions.data;

import java.util.logging.Logger;
import javax.sql.DataSource;

import org.ldg.sql.SQLInterface;

public class DAEWrapper extends SQLInterface<DataAccessException> {
    public DAEWrapper(DataSource source, Logger log, boolean debug) throws DataAccessException {
        super(source, log, debug);
    }

    public DataAccessException convertException(String message, Exception e) {
        return new DataAccessException(message, e);
    }
}
