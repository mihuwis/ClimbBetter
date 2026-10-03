package pl.climbbetter.training.internal.model;

public enum FamiliarityBand {
    FIRST_CONTACT,
    LOW,
    NORMAL,
    ESTABLISHED;


    public static FamiliarityBand fromPriorContactCount(int priorContactCount){
        if (priorContactCount < 0) {throw new IllegalArgumentException("Count cannot be negative");}
        if(priorContactCount == 0){
            return FIRST_CONTACT;
        } else if(priorContactCount <=10){
            return LOW;
        } else if(priorContactCount <=20){
            return NORMAL;
        } 
        return ESTABLISHED;
    }

}


