package com.wotifgroup.zkfss;

/**
 * Feature switch service interface for zkfss (https://github.com/lazjen/zkfss)
 * 
 * @author lazjen
 *
 */
public interface FeatureSwitchService {
    
    /**
     * Returns the value of the feature switch based on the key supplied.  
     * <br/>
     * The key name must follow these rules:<br/>
     *  1. not start with "/".  <br/>
     *  2. not contain the following characters: \u0001 - \u0019 and \u007F - \u009F, 
     *     \ud800 -uF8FFF, \uFFF0-uFFFF, \ uXFFFE - \ uXFFFF (where X is a digit 1 - E), \uF0000 - \uFFFFF.<br/>
     *  3. The "." character can be used as part of another name, but "." and ".." cannot alone be used<br/>
     *  
     * @param key Feature switch key value.
     * @return true if the feature is enabled.
     */
    boolean isEnabled(String key);
}
