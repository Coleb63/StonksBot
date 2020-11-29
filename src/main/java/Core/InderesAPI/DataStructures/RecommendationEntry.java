package Core.InderesAPI.DataStructures;

import com.google.gson.annotations.SerializedName;
import lombok.Getter;

/**
 * Single stock recommendation returned from Inderes API
 * @author etsubu
 */
@Getter
public class RecommendationEntry {
    private String isin;
    private String name;
    @SerializedName("date_of_recommendation")
    private String date;
    @SerializedName("target_price")
    private String target;
    private String currency;
    private String recommendation;

    /**
     * Converts numeric recommendation to descriptive text format
     * @return Stock recommendation in finnish
     */
    public String getRecommendationText() {
        if (recommendation == null || recommendation.isEmpty()) {
            return "tuntematon suositus";
        }
        switch (recommendation.charAt(0)) {
            case '1': return "myy";
            case '2': return "vähennä";
            case '4': return "lisää";
            case '5': return "osta";
            default: return "tuntematon suositus";
        }
    }
}
