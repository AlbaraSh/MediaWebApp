import { useCallback } from "react";
import { useQueryClient } from "@tanstack/react-query";
import { queryKeys } from "@/lib/queryKeys";

export function useInvalidateMediaCaches() {
  const queryClient = useQueryClient();

  return useCallback(
    () =>
      Promise.all([
        queryClient.invalidateQueries({ queryKey: queryKeys.media.all }),
        queryClient.invalidateQueries({ queryKey: queryKeys.userMedia.all }),
        queryClient.invalidateQueries({ queryKey: queryKeys.recommendations.all }),
      ]),
    [queryClient],
  );
}
