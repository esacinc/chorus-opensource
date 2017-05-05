package com.infoclinika.integration.skyline;

import com.infoclinika.msdata.image.MSSpectrum;
import edu.uw.gs.skyline.cloudapi.chromatogramgenerator.chromatogramrequest.ChromatogramRequestDocument;

import java.util.Arrays;

/**
 * @author Oleksii Tymchenko
 */
public class MSSpectrumExtractedItem {

    public final MSSpectrum spectrum;
    public final float[] mzErrors;
    public final ChromatogramRequestDocument.ChromatogramGroup.Chromatogram  chromatogram;

    public MSSpectrumExtractedItem(MSSpectrum spectrum, float[] mzErrors) {
        this.spectrum = spectrum;
        this.mzErrors = mzErrors;
        chromatogram = null;
    }

    public MSSpectrumExtractedItem(MSSpectrum spectrum, float[] mzErrors, ChromatogramRequestDocument.ChromatogramGroup.Chromatogram chromatogram) {
        this.spectrum = spectrum;
        this.mzErrors = mzErrors;
        this.chromatogram = chromatogram;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        MSSpectrumExtractedItem that = (MSSpectrumExtractedItem) o;

        if (spectrum != null ? !spectrum.equals(that.spectrum) : that.spectrum != null) return false;
        if (!Arrays.equals(mzErrors, that.mzErrors)) return false;
        return !(chromatogram != null ? !chromatogram.equals(that.chromatogram) : that.chromatogram != null);

    }

    @Override
    public int hashCode() {
        int result = spectrum != null ? spectrum.hashCode() : 0;
        result = 31 * result + (mzErrors != null ? Arrays.hashCode(mzErrors) : 0);
        result = 31 * result + (chromatogram != null ? chromatogram.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return "MSSpectrumExtractedItem{" +
                "spectrum=" + spectrum +
                ", mzErrors=" + Arrays.toString(mzErrors) +
                ", chromatogram=" + chromatogram +
                '}';
    }
}
