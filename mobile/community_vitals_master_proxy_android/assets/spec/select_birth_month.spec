P.1. Select Month [program: COMMUNITY VITALS REGISTRATION $$ action => javascript:selectBirthMonth()]
C.1. Community Vitals Registration birth month parameters
C.1. Special characters are:
C.1. "=>" for association
C.1. "$$" for separation of entries

Q.1.1. Select Year  [pos => 0 $$ field_type => number $$ validationRule => ^\d{4}$ $$ validationMessage => Wrong value. Expecting a 4 digit number!]
Q.1.2. Select Month [pos => 1 $$ tt_showToggleKeyboard => false $$ showKeys => false $$ dynamicLoader => loadMonthsFixed(__$('inputField').value.trim()) $$ tt_onLoad => loadMonthsFixed('') ]