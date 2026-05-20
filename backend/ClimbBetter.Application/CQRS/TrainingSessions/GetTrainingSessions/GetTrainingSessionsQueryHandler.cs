using ClimbBetter.Domain.Interfaces;
using MediatR;

namespace ClimbBetter.Application.CQRS.TrainingSessions.GetTrainingSessions;

public class GetTrainingSessionsQueryHandler
    : IRequestHandler<GetTrainingSessionsQuery, List<TrainingSessionListItemDto>>
{
    private readonly ITrainingSessionRepository _trainingSessionRepository;

    public GetTrainingSessionsQueryHandler(
        ITrainingSessionRepository trainingSessionRepository)
    {
        _trainingSessionRepository = trainingSessionRepository;
    }

    public async Task<List<TrainingSessionListItemDto>> Handle(
        GetTrainingSessionsQuery request,
        CancellationToken cancellationToken)
    {
        var sessions = await _trainingSessionRepository.GetAllAsync(cancellationToken);

        return sessions
            .Select(s => new TrainingSessionListItemDto
            {
                Id = s.Id,
                Date = s.Date,
                Duration = s.Duration,
                Goal = s.Goal,
                Method = s.Method,
                LocationId = s.LocationId,
                LocationName = s.Location?.Name,
                EntriesCount = s.Entries.Count,
                TotalMoves = s.Entries.Sum(e => e.ExecutedMoves),
                TotalPoints = s.Entries.Sum(e => e.PointsSnapshot)
            })
            .ToList();
    }
}