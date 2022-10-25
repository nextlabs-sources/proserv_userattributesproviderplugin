Ivy.jar is ivy 2.1 with a single modification

See https://issues.apache.org/jira/browse/IVY-439

Modified src/java/org/apache/ivy/util/filter/FilterHelper.java to be able to specify !{type} for post resolve tasks

    public static Filter getArtifactTypeFilter(String[] types) {
        if (types == null || types.length == 0) {
            return NO_FILTER;
        }
        List acceptedTypes = new ArrayList(types.length);
		List negatedTypes = new ArrayList(types.length);		
        for (int i = 0; i < types.length; i++) {
            String current = types[i].trim();
            if ("*".equals(current)) {
                return NO_FILTER;
            }
			
			if (current.startsWith("!"))
			{
				negatedTypes.add(current.substring(1));
			}
			else
			{
				acceptedTypes.add(current); 
			}
        }

		Filter acceptedTypesFilter = new ArtifactTypeFilter(acceptedTypes);
		Filter negatedTypesFilter = new NotFilter(new ArtifactTypeFilter(negatedTypes));

		return new AndFilter(acceptedTypesFilter, negatedTypesFilter);		
    }