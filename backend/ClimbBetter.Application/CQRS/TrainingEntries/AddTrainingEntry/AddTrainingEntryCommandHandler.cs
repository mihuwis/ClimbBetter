using ClimbBetter.Domain.Interfaces;
using ClimbBetter.Domain.Training;
using MediatR;

namespace ClimbBetter.Application.CQRS.TrainingEntries.AddTrainingEntry;

public class AddTrainingEntryCommandHandler
    : IRequestHandler<AddTrainingEntryCommand, Guid>
{
    private readonly ITrainingSessionRepository _trainingSessionRepository;
    private readonly IClimbRepository _climbRepository;
    private readonly IDifficultyRepository _difficultyRepository;
    private readonly IQualityRepository _qualityRepository;

    public AddTrainingEntryCommandHandler(
        ITrainingSessionRepository trainingSessionRepository,
        IClimbRepository climbRepository,
        IDifficultyRepository difficultyRepository,
        IQualityRepository qualityRepository)
    {
        _trainingSessionRepository = trainingSessionRepository;
        _climbRepository = climbRepository;
        _difficultyRepository = difficultyRepository;
        _qualityRepository = qualityRepository;
    }

    public async Task<Guid> Handle(
        AddTrainingEntryCommand request,
        CancellationToken cancellationToken)
    {
        var sessionExists = await _trainingSessionRepository.ExistsAsync(
            request.TrainingSessionId,
            cancellationToken);

        if (!sessionExists)
        {
            throw new InvalidOperationException("Training session does not exist.");
        }

        var climb = await _climbRepository.GetByIdAsync(
            request.ClimbId,
            cancellationToken);

        if (climb is null)
        {
            throw new InvalidOperationException("Climb does not exist.");
        }

        var difficulty = await _difficultyRepository.GetByIdAsync(
            request.DifficultyId,
            cancellationToken);

        if (difficulty is null)
        {
            throw new InvalidOperationException("Difficulty does not exist.");
        }

        var quality = await _qualityRepository.GetByIdAsync(
            request.QualityId,
            cancellationToken);

        if (quality is null)
        {
            throw new InvalidOperationException("Quality does not exist.");
        }

        var lengthMultiplier = CalculateLengthMultiplier(
            request.ExecutedMoves,
            request.TotalMoves ?? climb.SuggestedMoveCount);

        var points = difficulty.Points
            * quality.Multiplier
            * request.ExecutedMoves
            * lengthMultiplier;

        var trainingEntry = new TrainingEntry
        {
            Id = Guid.NewGuid(),
            TrainingSessionId = request.TrainingSessionId,
            ClimbId = request.ClimbId,
            DifficultyId = request.DifficultyId,
            QualityId = request.QualityId,
            AscentCategory = request.AscentCategory,
            IsSuccessful = request.IsSuccessful,
            AttemptNumberOnClimb = request.AttemptNumberOnClimb,
            ExecutedMoves = request.ExecutedMoves,
            TotalMoves = request.TotalMoves ?? climb.SuggestedMoveCount,
            LengthMultiplierSnapshot = lengthMultiplier,
            DifficultyPointsSnapshot = difficulty.Points,
            QualityMultiplierSnapshot = quality.Multiplier,
            PointsSnapshot = points,
            Notes = request.Notes,
            ClientEntryId = request.ClientEntryId,
            CreatedAt = DateTime.UtcNow
        };

        await _trainingSessionRepository.AddEntryAsync(
            trainingEntry,
            cancellationToken);

        return trainingEntry.Id;
    }

    private static decimal CalculateLengthMultiplier(
        int executedMoves,
        int totalMoves)
    {
        if (totalMoves <= 0)
        {
            return 1m;
        }

        var ratio = (decimal)executedMoves / totalMoves;

        return Math.Clamp(ratio, 0m, 1m);
    }
}