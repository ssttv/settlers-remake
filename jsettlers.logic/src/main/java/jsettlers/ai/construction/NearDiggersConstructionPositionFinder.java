/*******************************************************************************
 * Copyright (c) 2015 - 2017
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files (the "Software"),
 * to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense,
 * and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER
 * DEALINGS IN THE SOFTWARE.
 *******************************************************************************/
package jsettlers.ai.construction;

import static jsettlers.common.movable.EMovableType.DIGGER;

import java.util.ArrayList;
import java.util.List;

import jsettlers.ai.highlevel.AiStatistics;
import jsettlers.algorithms.construction.AbstractConstructionMarkableMap;
import jsettlers.common.buildings.EBuildingType;
import jsettlers.common.position.ShortPoint2D;

/**
 * Algorithm: find all possible construction points within the land of the player - calculates a score and take the position with the best score - score is affected by the distance to diggers - score
 * is affected by the effort to flattern
 *
 * @author codingberlin
 */
public class NearDiggersConstructionPositionFinder implements IBestConstructionPositionFinder {

	private EBuildingType buildingType;

	public NearDiggersConstructionPositionFinder(EBuildingType buildingType) {
		this.buildingType = buildingType;
	}

	@Override
	public ShortPoint2D findBestConstructionPosition(AiStatistics aiStatistics, AbstractConstructionMarkableMap constructionMap, byte playerId) {
		List<ShortPoint2D> diggers = aiStatistics.getPositionsOfMovablesWithTypeForPlayer(playerId, DIGGER);
		List<ScoredConstructionPosition> scoredConstructionPositions = new ArrayList<>();

		for (ShortPoint2D point : aiStatistics.getLandForPlayer(playerId)) {
			if (constructionMap.canConstructAt(point.x, point.y, buildingType, playerId)
					&& !aiStatistics.blocksWorkingAreaOfOtherBuilding(point.x, point.y, playerId, buildingType)) {
				ShortPoint2D nearestDiggerPosition = AiStatistics.detectNearestPointFromList(point, diggers);
				int nearestDiggerDistance = 0;
				if (nearestDiggerPosition != null) {
					nearestDiggerDistance = point.getOnGridDistTo(nearestDiggerPosition);
				}
				byte flatternEffort = aiStatistics.getFlatternEffortAtPositionForBuilding(point, buildingType);
				scoredConstructionPositions.add(new ScoredConstructionPosition(point, nearestDiggerDistance + flatternEffort));
			}
		}

		return ScoredConstructionPosition.detectPositionWithLowestScore(scoredConstructionPositions);
	}
}
