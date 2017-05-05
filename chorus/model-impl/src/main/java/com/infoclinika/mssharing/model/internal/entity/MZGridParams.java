package com.infoclinika.mssharing.model.internal.entity;

import com.infoclinika.msdata.image.GridType;
import org.springframework.data.jpa.domain.AbstractPersistable;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.OneToOne;

/**
 * @author vladislav.kovchug
 */
@Entity
public class MZGridParams extends AbstractPersistable<Long> {

    @Column
    private GridType gridType;

    @Column
    private Integer mzStart;

    @Column
    private Integer mzEnd;

    @Column
    private String params;

    @Column
    private Integer step;

    @OneToOne(mappedBy = "mzGridParams")
    private MSFunctionItem msFunctionItem;

    /*package*/ public MZGridParams() {
    }

    public MZGridParams(GridType gridType, Integer mzStart, Integer mzEnd, String params, Integer step) {
        this.gridType = gridType;
        this.mzStart = mzStart;
        this.mzEnd = mzEnd;
        this.params = params;
        this.step = step;
    }

    public MZGridParams(GridType gridType, Integer mzStart, Integer mzEnd, String params, Integer step, MSFunctionItem msFunctionItem) {
        this.gridType = gridType;
        this.mzStart = mzStart;
        this.mzEnd = mzEnd;
        this.params = params;
        this.step = step;
        this.msFunctionItem = msFunctionItem;
    }

    public GridType getGridType() {
        return gridType;
    }

    public void setGridType(GridType gridType) {
        this.gridType = gridType;
    }

    public Integer getMzStart() {
        return mzStart;
    }

    public void setMzStart(Integer mzStart) {
        this.mzStart = mzStart;
    }

    public Integer getMzEnd() {
        return mzEnd;
    }

    public void setMzEnd(Integer mzEnd) {
        this.mzEnd = mzEnd;
    }

    public String getParams() {
        return params;
    }

    public void setParams(String params) {
        this.params = params;
    }

    public Integer getStep() {
        return step;
    }

    public void setStep(Integer step) {
        this.step = step;
    }

    public MSFunctionItem getMsFunctionItem() {
        return msFunctionItem;
    }

    public void setMsFunctionItem(MSFunctionItem msFunctionItem) {
        this.msFunctionItem = msFunctionItem;
    }

    @Override
    public String toString() {
        return "MZGridParams{" +
                "gridType=" + gridType +
                ", mzStart=" + mzStart +
                ", mzEnd=" + mzEnd +
                ", params='" + params + '\'' +
                ", step=" + step +
                "} " + super.toString();
    }
}
