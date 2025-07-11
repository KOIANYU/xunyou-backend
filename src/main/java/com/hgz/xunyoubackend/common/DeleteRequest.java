package com.hgz.xunyoubackend.common;

import lombok.Data;

import java.io.Serializable;

@Data
public class DeleteRequest implements Serializable {

    private static final long serialVersionUID = 8749279770600982526L;

    private long id;
}
